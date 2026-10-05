package com.lifeforce.payer.request.service;

import com.lifeforce.payer.plan.domain.BenefitStatus;
import com.lifeforce.payer.plan.domain.Plan;
import com.lifeforce.payer.plan.domain.PlanService;
import com.lifeforce.payer.reference.domain.NetworkParticipation;
import com.lifeforce.payer.reference.repository.NetworkParticipationRepository;
import com.lifeforce.payer.reference.repository.OrganizationRepository;
import com.lifeforce.payer.reference.repository.PatientRepository;
import com.lifeforce.payer.reference.repository.ProviderRepository;
import com.lifeforce.payer.request.domain.*;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.HttpAuthorizationResponse;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.repository.CoverageRepository;
import com.lifeforce.payer.plan.repository.PlanRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthorizationRequestService {
    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final CoverageRepository coverageRepository;
    private final PlanRepository planRepository;
    private final ReviewRepository reviewRepository;
    private final NetworkParticipationRepository networkParticipationRepository;
    private final OrganizationRepository organizationRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final Clock clock;
    public AuthorizationRequestService(AuthorizationRequestRepository authorizationRequestRepository, CoverageRepository coverageRepository, PlanRepository planRepository, ReviewRepository reviewRepository, NetworkParticipationRepository networkParticipationRepository, OrganizationRepository organizationRepository, PatientRepository patientRepository, ProviderRepository providerRepository, Clock clock) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.coverageRepository = coverageRepository;
        this.planRepository = planRepository;
        this.reviewRepository = reviewRepository;
        this.networkParticipationRepository = networkParticipationRepository;
        this.organizationRepository = organizationRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.clock = clock;
    }

    @Transactional
    public HttpAuthorizationResponse createAuthorizationRequest(HttpAuthorizationRequest request) {
        boolean isDuplicate = authorizationRequestRepository.existsById(request.requestId());
        if (isDuplicate) {
            return new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.DUPLICATE_REJECTED,
                    "Request has already been submitted"
            );
        }

        if (!patientRepository.existsById(request.patientId())) {
            return new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.MALFORMED_REQUEST,
                    "Patient does not exist"
            );
        }

        if (!organizationRepository.existsById(request.organizationId())) {
            return new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.MALFORMED_REQUEST,
                    "Organization does not exist"
            );
        }

        if (!providerRepository.existsByIdAndOrganizationId(request.providerId(), request.organizationId())) {
            return new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.MALFORMED_REQUEST,
                    "Provider does not exist or does not belong to organization"
            );
        }

        if (!planRepository.existsById(request.planId())) {
            return new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.MALFORMED_REQUEST,
                    "Plan does not exist"
            );
        }

        AuthorizationRequest authorizationRequest = new AuthorizationRequest().build(request);
        authorizationRequestRepository.save(authorizationRequest);

        return new HttpAuthorizationResponse(
                authorizationRequest.getId(),
                ResponseStatus.SUBMITTED,
                "Request has been submitted"
        );
    }

    @Transactional
    public void processSubmittedRequest(UUID requestId) {
        Optional<AuthorizationRequest> authorizationRequest = authorizationRequestRepository.findById(requestId);
        if(authorizationRequest.isEmpty()) {
            return;
        }
        AuthorizationRequest request = authorizationRequest.get();
        if (!Status.SUBMITTED.equals(request.getStatus())) {
            return;
        }

        // CHECK PATIENT'S COVERAGE
        List<Coverage> coverages = coverageRepository.findByPatientIdAndPlanId(request.getPatientId(), request.getPlanId());
        if (coverages.isEmpty()) {
            request.updateStatus(Status.REJECTED, StatusReason.NOT_COVERED);
            authorizationRequestRepository.save(request);
            return;
        }

        Coverage currentCoverage = coverages.stream().filter(
            coverage -> (
                    !coverage.getStart().isAfter(request.getSubmittedAt()) &&
                    (
                            coverage.getEnd() == null ||
                            coverage.getEnd().isAfter(request.getSubmittedAt())
                    )
            )
        ).findFirst().orElse(null);

        if (currentCoverage == null) {
            request.updateStatus(Status.REJECTED, StatusReason.COVERAGE_INACTIVE);
            authorizationRequestRepository.save(request);
            return;
        }

        // CHECK SERVICE COVERED IN PLAN
        Optional<Plan> plan = planRepository.findById(request.getPlanId());
        if (plan.isEmpty()) {
            // CREATE MANUAL REVIEW FOR UNCONFIGURED PLAN
            Review review = Review.createNewManualReview(request.getId(), clock);
            reviewRepository.save(review);

            request.updateStatus(Status.PENDING, StatusReason.MANUAL_REVIEW_REQUIRED);
            authorizationRequestRepository.save(request);
            return;
        }
        Plan currentPlan = plan.get();
        List<NetworkParticipation> networkParticipations = networkParticipationRepository.findByPayerIdAndOrganizationId(currentPlan.getPayerId(), request.getOrganizationId());
        boolean isInNetwork = networkParticipations.stream().anyMatch(
                networkParticipation -> (
                        networkParticipation.isInNetwork() &&
                        !networkParticipation.getEffectiveFrom().isAfter(request.getSubmittedAt()) &&
                        (
                                networkParticipation.getEffectiveTo() == null ||
                                networkParticipation.getEffectiveTo().isAfter(request.getSubmittedAt())
                        )
                )
        );
        if (!isInNetwork) {
            request.updateStatus(Status.REJECTED, StatusReason.OUT_OF_NETWORK);
            authorizationRequestRepository.save(request);
            return;
        }

        RequestedService requestedService = request.getRequestedService();
        List<PlanService> matchingServices = currentPlan.getPlanServices().stream().filter(
                planService -> planService.getCode().equals(requestedService.code())
        ).toList();
        if (matchingServices.isEmpty()) {
            Review review = Review.createNewManualReview(request.getId(), clock);
            reviewRepository.save(review);

            request.updateStatus(Status.PENDING, StatusReason.MANUAL_REVIEW_REQUIRED);
            authorizationRequestRepository.save(request);
            return;
        }

        Optional<PlanService> matchingService = matchingServices.stream().filter(planService -> planService.getBenefitStatus().equals(BenefitStatus.COVERED)).findFirst();
        if (matchingService.isEmpty()) {
            request.updateStatus(Status.REJECTED, StatusReason.SERVICE_EXCLUDED);
            authorizationRequestRepository.save(request);
            return;
        }

        // CHECK IF PRIOR AUTH IS REQUIRED
        PlanService currentService = matchingService.get();
        if (!currentService.getPriorAuthorizationRequired()) {
            request.updateStatus(Status.APPROVED, StatusReason.PRIOR_AUTH_NOT_REQUIRED);
            authorizationRequestRepository.save(request);
            return;
        }

        // CREATE A REVIEW FOR PRIOR AUTH REQUIRED CRITERIA
        Review review = Review.createNewReview(request.getId(), clock);
        reviewRepository.save(review);

        request.updateStatus(Status.PENDING, StatusReason.PENDING_EVALUATION);
        authorizationRequestRepository.save(request);
    }

}

package com.lifeforce.payer.request.service;

import com.lifeforce.payer.plan.domain.plan.BenefitStatus;
import com.lifeforce.payer.plan.domain.plan.Plan;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.reference.domain.NetworkParticipation;
import com.lifeforce.payer.reference.repository.NetworkParticipationRepository;
import com.lifeforce.payer.reference.repository.OrganizationRepository;
import com.lifeforce.payer.reference.repository.PatientRepository;
import com.lifeforce.payer.reference.repository.ProviderRepository;
import com.lifeforce.payer.request.domain.*;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.HttpAuthorizationResponse;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.repository.CoverageRepository;
import com.lifeforce.payer.plan.repository.PlanRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has already been submitted");
        }

        if (!patientRepository.existsById(request.patientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient does not exist");
        }

        if (!organizationRepository.existsById(request.organizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization does not exist");
        }

        if (!providerRepository.existsByIdAndOrganizationId(request.providerId(), request.organizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider does not exist or does not belong to organization");
        }

        if (!planRepository.existsById(request.planId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan does not exist");
        }

        AuthorizationRequest authorizationRequest = request.toDomain();
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
        if (!RequestStatus.SUBMITTED.equals(request.getRequestStatus())) {
            return;
        }

        // CHECK PATIENT'S COVERAGE
        List<Coverage> coverages = coverageRepository.findByPatientIdAndPlanId(request.getPatientId(), request.getPlanId());
        if (coverages.isEmpty()) {
            request.updateStatusToNotCovered();
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
            request.updateStatusToCoverageInactive();
            authorizationRequestRepository.save(request);
            return;
        }

        // CHECK SERVICE COVERED IN PLAN
        Optional<Plan> plan = planRepository.findById(request.getPlanId());
        if (plan.isEmpty()) {
            // CREATE MANUAL REVIEW FOR UNCONFIGURED PLAN
            Review review = Review.createNewManualReview(request.getId(), clock);
            request.updateStatusToManualReview();
            authorizationRequestRepository.save(request);
            reviewRepository.save(review);
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
            request.updateStatusToOutOfNetwork();
            authorizationRequestRepository.save(request);
            return;
        }

        RequestedService requestedService = request.getRequestedService();
        List<PlanService> matchingServices = currentPlan.getPlanServices().stream().filter(
                planService -> planService.getCode().equals(requestedService.code())
        ).toList();
        if (matchingServices.isEmpty()) {
            Review review = Review.createNewManualReview(request.getId(), clock);
            request.updateStatusToManualReview();
            authorizationRequestRepository.save(request);
            reviewRepository.save(review);
            return;
        }

        Optional<PlanService> matchingService = matchingServices.stream().filter(planService -> planService.getBenefitStatus().equals(BenefitStatus.COVERED)).findFirst();
        if (matchingService.isEmpty()) {
            request.updateStatusToServiceExcluded();
            authorizationRequestRepository.save(request);
            return;
        }

        // CHECK IF PRIOR AUTH IS REQUIRED
        PlanService currentService = matchingService.get();
        if (!currentService.getPriorAuthorizationRequired()) {
            request.updateStatusToPriorAuthNotRequired();
            authorizationRequestRepository.save(request);
            return;
        }

        // CREATE A REVIEW FOR PRIOR AUTH REQUIRED CRITERIA
        Review review = Review.createNewReview(request.getId(), clock);
        request.updateStatusToPendingEvaluation();
        authorizationRequestRepository.save(request);
        reviewRepository.save(review);
    }
}

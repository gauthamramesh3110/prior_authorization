package com.lifeforce.payer.review.service;

import com.lifeforce.payer.plan.domain.plan.CodeType;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.plan.service.PolicyEvaluationResult;
import com.lifeforce.payer.plan.repository.PlanServiceRepository;
import com.lifeforce.payer.plan.service.PolicyEvaluationService;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    private final PolicyEvaluationService policyEvaluationService;
    private final ReviewRepository reviewRepository;
    private final PlanServiceRepository planServiceRepository;
    private final Clock clock;
    private final AuthorizationRequestRepository authorizationRequestRepository;
    public ReviewService(PolicyEvaluationService policyEvaluationService, ReviewRepository reviewRepository, PlanServiceRepository planServiceRepository, AuthorizationRequestRepository authorizationRequestRepository, Clock clock) {
        this.policyEvaluationService = policyEvaluationService;
        this.reviewRepository = reviewRepository;
        this.planServiceRepository = planServiceRepository;
        this.clock = clock;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Transactional
    public void evaluateReview(UUID reviewId) {
        Optional<AuthorizationRequest> request = authorizationRequestRepository.findByReviewIdForUpdate(reviewId);
        if (request.isEmpty()) {
            return;
        }
        Optional<Review> review = reviewRepository.findById(reviewId);
        if (review.isEmpty()) {
            return;
        }
        Review currentReview = review.get();
        if (currentReview.getReviewStatus() != ReviewStatus.PENDING_EVALUATION) {
            return;
        }

        AuthorizationRequest authorizationRequest = request.get();
        RequestedService requestedService = authorizationRequest.getRequestedService();
        Optional<PlanService> planService = planServiceRepository.findByPlanIdAndCodeAndCodeType(authorizationRequest.getPlanId(), requestedService.code(), CodeType.PROCEDURE);
        if (planService.isEmpty()) {
            authorizationRequest.updateStatusToManualReview();
            currentReview.updateStatusToManualReview(clock);
            saveReview(currentReview);
            return;
        }

        PolicyEvaluationResult result = policyEvaluationService.evaluatePolicy(planService.get().getPolicy(), authorizationRequest.getClinicalJustification(), authorizationRequest.getEvidenceEvaluationAt());
        if (result == PolicyEvaluationResult.MATCHED) {
            authorizationRequest.updateStatusToAutoApproved();
            currentReview.updateStatusToAutoApproved(requestedService.quantity(), clock);
            saveReview(currentReview);
            return;
        }
        if (result == PolicyEvaluationResult.AWAITING_EVIDENCE) {
            authorizationRequest.updateStatusToAwaitingEvidence();
            currentReview.requestEvidence(policyEvaluationService.getRequestedEvidence(
                    planService.get().getPolicy(), authorizationRequest.getClinicalJustification(), authorizationRequest.getEvidenceEvaluationAt()
            ), clock);
            saveReview(currentReview);
            return;
        }

        if (result == PolicyEvaluationResult.CRITERIA_NOT_MET) {
            authorizationRequest.updateStatusToCriteriaNotMet();
        } else {
            authorizationRequest.updateStatusToManualReview();
        }
        currentReview.updateStatusToManualReview(clock);
        saveReview(currentReview);
    }

    private void saveReview(Review review) {
        authorizationRequestRepository.save(review.getAuthorizationRequest());
        reviewRepository.save(review);
    }
}

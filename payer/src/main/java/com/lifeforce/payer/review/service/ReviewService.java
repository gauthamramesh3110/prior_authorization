package com.lifeforce.payer.review.service;

import com.lifeforce.payer.plan.domain.plan.CodeType;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.plan.service.PolicyEvaluationResult;
import com.lifeforce.payer.plan.repository.PlanServiceRepository;
import com.lifeforce.payer.plan.service.PlanEvalService;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    private final PlanEvalService planEvalService;
    private final ReviewRepository reviewRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final PlanServiceRepository planServiceRepository;
    private final Clock clock;
    public ReviewService(PlanEvalService planEvalService, ReviewRepository reviewRepository, ReviewHistoryRepository reviewHistoryRepository, PlanServiceRepository planServiceRepository, Clock clock) {
        this.planEvalService = planEvalService;
        this.reviewRepository = reviewRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.planServiceRepository = planServiceRepository;
        this.clock = clock;
    }

    @Transactional
    public void evaluateReview(UUID reviewId) {
        Optional<Review> review = reviewRepository.findById(reviewId);
        if (review.isEmpty()) {
            return;
        }
        Review currentReview = review.get();
        if (currentReview.getReviewStatus() != ReviewStatus.PENDING_EVALUATION) {
            return;
        }

        AuthorizationRequest authorizationRequest = currentReview.getAuthorizationRequest();
        RequestedService requestedService = authorizationRequest.getRequestedService();
        Optional<PlanService> planService = planServiceRepository.findByPlanIdAndCodeAndCodeType(authorizationRequest.getPlanId(), requestedService.code(), CodeType.PROCEDURE);
        if (planService.isEmpty()) {
            authorizationRequest.updateStatusToManualReview();
            currentReview.updateStatusToManualReview(clock);
            saveReview(currentReview);
            return;
        }

        PolicyEvaluationResult result = planEvalService.evalPolicyForEvidence(planService.get().getPolicy(), authorizationRequest.getClinicalJustification(), authorizationRequest.getSubmittedAt());
        if (result == PolicyEvaluationResult.MATCHED) {
            authorizationRequest.updateStatusToAutoApproved();
            currentReview.updateStatusToAutoApproved(requestedService.quantity(), clock);
            saveReview(currentReview);
            return;
        }
        if (result == PolicyEvaluationResult.AWAITING_EVIDENCE) {
            authorizationRequest.updateStatusToAwaitingEvidence();
            currentReview.updateStatusToAwaitingEvidence(clock);
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
        reviewRepository.save(review);
        reviewHistoryRepository.save(ReviewHistory.createSystemEvent(review));
    }
}

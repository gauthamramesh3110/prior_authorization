package com.lifeforce.payer.review.service;

import com.lifeforce.payer.plan.domain.plan.CodeType;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.repository.PlanServiceRepository;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.dto.ReviewSummary;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewQueryService {
    private final ReviewRepository reviewRepository;
    private final PlanServiceRepository planServiceRepository;

    public ReviewQueryService(ReviewRepository reviewRepository, PlanServiceRepository planServiceRepository) {
        this.reviewRepository = reviewRepository;
        this.planServiceRepository = planServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewSummary> getReviews(ReviewStatus reviewStatus) {
        return reviewRepository.findByReviewStatusOrderByLastUpdatedAscIdAsc(reviewStatus).stream()
                .map(ReviewSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReviewDetails> getReviewDetails(UUID reviewId) {
        Optional<Review> review = reviewRepository.findById(reviewId);
        if (review.isEmpty()) {
            return Optional.empty();
        }

        Review currentReview = review.get();
        AuthorizationRequest request = currentReview.getAuthorizationRequest();
        Policy policy = planServiceRepository.findByPlanIdAndCodeAndCodeType(
                request.getPlanId(), request.getRequestedService().code(), CodeType.PROCEDURE
        ).map(PlanService::getPolicy).orElse(null);

        return Optional.of(ReviewDetails.from(currentReview, policy));
    }
}

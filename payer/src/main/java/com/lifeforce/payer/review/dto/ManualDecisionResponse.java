package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ManualDecisionResponse(
        UUID id,
        UUID requestId,
        ReviewStatus reviewStatus,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason,
        Decision decision,
        String decisionReason,
        Instant decisionDate,
        DecisionActor decidedBy,
        UUID reviewerId,
        Integer approvedQuantity,
        Instant validFrom,
        Instant validTo
) {
    public static ManualDecisionResponse from(Review review) {
        return new ManualDecisionResponse(
                review.getId(), review.getRequestId(), review.getReviewStatus(),
                review.getAuthorizationRequest().getRequestStatus(), review.getAuthorizationRequest().getRequestStatusReason(),
                review.getDecision(), review.getDecisionReason(), review.getDecisionDate(), review.getDecidedBy(),
                review.getReviewerId(), review.getApprovedQuantity(), review.getValidFrom(), review.getValidTo()
        );
    }
}

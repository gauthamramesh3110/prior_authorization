package com.lifeforce.payer.request.dto;

import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record EvidenceSubmissionResponse(
        UUID id,
        UUID requestId,
        UUID reviewId,
        UUID providerId,
        ReviewStatus reviewStatus,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason,
        Instant evidenceUpdatedAt
) {
    public static EvidenceSubmissionResponse from(Review review, ReviewHistory history) {
        return new EvidenceSubmissionResponse(
                history.getId(), review.getRequestId(), review.getId(),
                review.getAuthorizationRequest().getProviderId(), review.getReviewStatus(),
                review.getAuthorizationRequest().getRequestStatus(),
                review.getAuthorizationRequest().getRequestStatusReason(),
                review.getAuthorizationRequest().getEvidenceUpdatedAt()
        );
    }
}

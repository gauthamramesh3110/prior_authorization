package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record EvidenceRequestResponse(
        UUID id,
        UUID reviewId,
        UUID requestId,
        UUID reviewerId,
        RequestedEvidence evidenceRequest,
        Instant requestedAt,
        ReviewStatus reviewStatus,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason
) {
    public static EvidenceRequestResponse from(Review review, ReviewHistory history) {
        return new EvidenceRequestResponse(
                history.getId(), review.getId(), review.getRequestId(), review.getReviewerId(),
                RequestedEvidence.from(review.getEvidenceRequest()), history.getEventAt(), review.getReviewStatus(),
                review.getAuthorizationRequest().getRequestStatus(), review.getAuthorizationRequest().getRequestStatusReason()
        );
    }
}

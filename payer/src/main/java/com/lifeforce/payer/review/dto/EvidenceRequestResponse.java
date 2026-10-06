package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record EvidenceRequestResponse(
        UUID reviewId,
        UUID requestId,
        UUID reviewerId,
        RequestedEvidence evidenceRequest,
        Instant requestedAt,
        ReviewStatus reviewStatus,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason
) {
    public static EvidenceRequestResponse from(Review review) {
        return new EvidenceRequestResponse(
                review.getId(), review.getRequestId(), review.getReviewerId(),
                RequestedEvidence.from(review.getEvidenceRequest()), review.getLastUpdated(), review.getReviewStatus(),
                review.getAuthorizationRequest().getRequestStatus(), review.getAuthorizationRequest().getRequestStatusReason()
        );
    }
}

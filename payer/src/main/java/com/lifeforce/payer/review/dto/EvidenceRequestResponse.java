package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceRequestResponse(
        UUID id,
        UUID reviewId,
        UUID requestId,
        UUID reviewerId,
        String message,
        List<String> requestedEvidence,
        Instant requestedAt,
        ReviewStatus reviewStatus,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason
) {
    public static EvidenceRequestResponse from(Review review, ReviewHistory history, EvidenceRequest request) {
        return new EvidenceRequestResponse(
                history.getId(), review.getId(), review.getRequestId(), review.getReviewerId(), request.message(),
                List.copyOf(request.requestedEvidence()), history.getEventAt(), review.getReviewStatus(),
                review.getAuthorizationRequest().getRequestStatus(), review.getAuthorizationRequest().getRequestStatusReason()
        );
    }
}

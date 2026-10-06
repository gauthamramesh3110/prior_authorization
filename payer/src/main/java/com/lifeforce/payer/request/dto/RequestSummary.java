package com.lifeforce.payer.request.dto;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record RequestSummary(
        UUID id,
        UUID patientId,
        UUID providerId,
        UUID organizationId,
        UUID planId,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason,
        RequestedService requestedService,
        Instant submittedAt,
        Instant evidenceUpdatedAt,
        UUID reviewId,
        ReviewStatus reviewStatus
) {
    public static RequestSummary from(AuthorizationRequest request, Review review) {
        return new RequestSummary(
                request.getId(), request.getPatientId(), request.getProviderId(), request.getOrganizationId(),
                request.getPlanId(), request.getRequestStatus(), request.getRequestStatusReason(),
                RequestedService.from(request.getRequestedService()), request.getSubmittedAt(), request.getEvidenceUpdatedAt(),
                review == null ? null : review.getId(), review == null ? null : review.getReviewStatus()
        );
    }
}

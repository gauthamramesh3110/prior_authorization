package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewSummary(
        UUID id,
        UUID requestId,
        ReviewStatus reviewStatus,
        RequestStatus requestStatus,
        RequestStatusReason requestStatusReason,
        UUID patientId,
        UUID providerId,
        UUID organizationId,
        UUID planId,
        RequestedService requestedService,
        Instant submittedAt,
        Instant lastUpdated
) {
    public static ReviewSummary from(Review review) {
        AuthorizationRequest request = review.getAuthorizationRequest();
        return new ReviewSummary(
                review.getId(), request.getId(), review.getReviewStatus(), request.getRequestStatus(),
                request.getRequestStatusReason(), request.getPatientId(), request.getProviderId(),
                request.getOrganizationId(), request.getPlanId(), request.getRequestedService(),
                request.getSubmittedAt(), review.getLastUpdated()
        );
    }
}

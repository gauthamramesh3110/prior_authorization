package com.lifeforce.payer.request.dto;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.RequestedEvidence;

import java.time.Instant;
import java.util.UUID;

public record RequestDetails(
        RequestSummary request,
        ClinicalJustification clinicalJustification,
        ReviewDetails review
) {
    public static RequestDetails from(AuthorizationRequest request, Review review) {
        return new RequestDetails(
                RequestSummary.from(request, review), ClinicalJustification.from(request.getClinicalJustification()),
                review == null ? null : ReviewDetails.from(review)
        );
    }

    public record ReviewDetails(
            UUID id,
            ReviewStatus reviewStatus,
            Instant lastUpdated,
            Decision decision,
            String decisionReason,
            Instant decisionDate,
            DecisionActor decidedBy,
            UUID reviewerId,
            Integer approvedQuantity,
            Instant validFrom,
            Instant validTo,
            RequestedEvidence evidenceRequest
    ) {
        public static ReviewDetails from(Review review) {
            return new ReviewDetails(
                    review.getId(), review.getReviewStatus(), review.getLastUpdated(), review.getDecision(),
                    review.getDecisionReason(), review.getDecisionDate(), review.getDecidedBy(), review.getReviewerId(),
                    review.getApprovedQuantity(), review.getValidFrom(), review.getValidTo(),
                    RequestedEvidence.from(review.getEvidenceRequest())
            );
        }
    }
}

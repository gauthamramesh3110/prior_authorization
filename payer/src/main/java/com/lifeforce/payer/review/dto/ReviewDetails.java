package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.plan.domain.policy.EvidenceType;
import com.lifeforce.payer.plan.domain.policy.Match;
import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.domain.policy.PolicyCriterion;
import com.lifeforce.payer.plan.domain.policy.PolicyCriterionOperator;
import com.lifeforce.payer.plan.domain.policy.ReviewMode;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewDetails(
        UUID id,
        ReviewStatus reviewStatus,
        Instant lastUpdated,
        Decision decision,
        String decisionReason,
        Instant decisionDate,
        DecisionActor decidedBy,
        UUID reviewerId,
        Instant validFrom,
        Instant validTo,
        Integer approvedQuantity,
        RequestDetails request,
        PolicyDetails policy
) {
    public static ReviewDetails from(Review review, Policy policy) {
        return new ReviewDetails(
                review.getId(), review.getReviewStatus(), review.getLastUpdated(), review.getDecision(),
                review.getDecisionReason(), review.getDecisionDate(), review.getDecidedBy(), review.getReviewerId(),
                review.getValidFrom(), review.getValidTo(), review.getApprovedQuantity(),
                RequestDetails.from(review.getAuthorizationRequest()), policy == null ? null : PolicyDetails.from(policy)
        );
    }

    public record RequestDetails(
            UUID id,
            UUID patientId,
            UUID providerId,
            UUID organizationId,
            UUID planId,
            RequestStatus requestStatus,
            RequestStatusReason requestStatusReason,
            RequestedService requestedService,
            ClinicalJustification clinicalJustification,
            Instant submittedAt
    ) {
        public static RequestDetails from(AuthorizationRequest request) {
            return new RequestDetails(
                    request.getId(), request.getPatientId(), request.getProviderId(), request.getOrganizationId(),
                    request.getPlanId(), request.getRequestStatus(), request.getRequestStatusReason(),
                    RequestedService.from(request.getRequestedService()), ClinicalJustification.from(request.getClinicalJustification()), request.getSubmittedAt()
            );
        }
    }

    public record PolicyDetails(
            UUID id,
            ReviewMode reviewMode,
            Match match,
            List<CriterionDetails> criteria
    ) {
        public static PolicyDetails from(Policy policy) {
            return new PolicyDetails(
                    policy.getId(), policy.getReviewMode(), policy.getMatch(),
                    policy.getPolicyCriteria().stream().map(CriterionDetails::from).toList()
            );
        }
    }

    public record CriterionDetails(
            UUID id,
            EvidenceType evidenceType,
            String code,
            PolicyCriterionOperator operator,
            BigDecimal value,
            String unit
    ) {
        public static CriterionDetails from(PolicyCriterion criterion) {
            return new CriterionDetails(
                    criterion.getId(), criterion.getEvidenceType(), criterion.getCode(),
                    criterion.getOperator(), criterion.getValue(), criterion.getUnit()
            );
        }
    }
}

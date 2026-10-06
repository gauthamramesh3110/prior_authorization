package com.lifeforce.payer.request.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity(name = "authorization_request")
public class AuthorizationRequest {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    UUID patientId;

    @Column(name = "provider_id", nullable = false)
    UUID providerId;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "plan_id", nullable = false)
    UUID planId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    RequestStatus requestStatus;

    @Column(name = "status_reason")
    @Enumerated(EnumType.STRING)
    RequestStatusReason requestStatusReason;

    @Column(name = "requested_service", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    RequestedService requestedService;

    @Column(name = "clinical_justification", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    ClinicalJustification clinicalJustification;

    @Column(name = "submitted_at")
    Instant submittedAt;

    @Column(name = "evidence_updated_at")
    Instant evidenceUpdatedAt;

    public Instant getEvidenceEvaluationAt() {
        return evidenceUpdatedAt == null ? submittedAt : evidenceUpdatedAt;
    }

    public void addEvidence(ClinicalJustification evidence, Clock clock) {
        ClinicalJustification currentEvidence = clinicalJustification == null
                ? new ClinicalJustification(null, null, null) : clinicalJustification;
        String summary = currentEvidence.summary();
        if (evidence.summary() != null && !evidence.summary().isBlank()) {
            summary = summary == null || summary.isBlank() ? evidence.summary() : summary + "\n\n" + evidence.summary();
        }
        List<ClinicalJustification.ConditionEvidence> conditions = new ArrayList<>();
        if (currentEvidence.conditions() != null) {
            conditions.addAll(currentEvidence.conditions());
        }
        if (evidence.conditions() != null) {
            conditions.addAll(evidence.conditions());
        }
        List<ClinicalJustification.ObservationEvidence> observations = new ArrayList<>();
        if (currentEvidence.observations() != null) {
            observations.addAll(currentEvidence.observations());
        }
        if (evidence.observations() != null) {
            observations.addAll(evidence.observations());
        }
        this.clinicalJustification = new ClinicalJustification(summary, List.copyOf(conditions), List.copyOf(observations));
        this.evidenceUpdatedAt = Instant.now(clock);
        updateStatusToEvidenceUpdated();
    }

    public void updateStatusToEvidenceUpdated() {
        this.requestStatus = RequestStatus.PENDING;
        this.requestStatusReason = RequestStatusReason.EVIDENCE_UPDATED;
    }

    public AuthorizationRequest build(UUID requestId, Instant submittedAt, UUID patientId, UUID providerId, UUID organizationId, UUID planId, RequestedService requestedService, ClinicalJustification clinicalJustification) {
        this.id = requestId;
        this.patientId = patientId;
        this.providerId = providerId;
        this.organizationId = organizationId;
        this.planId = planId;
        updateStatusToSubmitted();
        this.requestedService = requestedService;
        this.clinicalJustification = clinicalJustification;
        this.submittedAt = submittedAt;
        return this;
    }

    public void updateStatusToSubmitted() {
        this.requestStatus = RequestStatus.SUBMITTED;
        this.requestStatusReason = null;
    }

    public void updateStatusToPendingEvaluation() {
        this.requestStatus = RequestStatus.PENDING;
        this.requestStatusReason = RequestStatusReason.PENDING_EVALUATION;
    }

    public void updateStatusToManualReview() {
        this.requestStatus = RequestStatus.PENDING;
        this.requestStatusReason = RequestStatusReason.MANUAL_REVIEW_REQUIRED;
    }

    public void updateStatusToAwaitingEvidence() {
        this.requestStatus = RequestStatus.PENDING;
        this.requestStatusReason = RequestStatusReason.AWAITING_EVIDENCE;
    }

    public void updateStatusToCriteriaNotMet() {
        this.requestStatus = RequestStatus.PENDING;
        this.requestStatusReason = RequestStatusReason.CRITERIA_NOT_MET;
    }

    public void updateStatusToAutoApproved() {
        this.requestStatus = RequestStatus.APPROVED;
        this.requestStatusReason = RequestStatusReason.AUTO_APPROVED;
    }

    public void updateStatusToManuallyApproved() {
        this.requestStatus = RequestStatus.APPROVED;
        this.requestStatusReason = RequestStatusReason.MANUAL_APPROVED;
    }

    public void updateStatusToPriorAuthNotRequired() {
        this.requestStatus = RequestStatus.APPROVED;
        this.requestStatusReason = RequestStatusReason.PRIOR_AUTH_NOT_REQUIRED;
    }

    public void updateStatusToNotCovered() {
        this.requestStatus = RequestStatus.REJECTED;
        this.requestStatusReason = RequestStatusReason.NOT_COVERED;
    }

    public void updateStatusToCoverageInactive() {
        this.requestStatus = RequestStatus.REJECTED;
        this.requestStatusReason = RequestStatusReason.COVERAGE_INACTIVE;
    }

    public void updateStatusToOutOfNetwork() {
        this.requestStatus = RequestStatus.REJECTED;
        this.requestStatusReason = RequestStatusReason.OUT_OF_NETWORK;
    }

    public void updateStatusToServiceExcluded() {
        this.requestStatus = RequestStatus.REJECTED;
        this.requestStatusReason = RequestStatusReason.SERVICE_EXCLUDED;
    }

    public void updateStatusToManuallyRejected() {
        this.requestStatus = RequestStatus.REJECTED;
        this.requestStatusReason = RequestStatusReason.MANUAL_REJECTED;
    }
}

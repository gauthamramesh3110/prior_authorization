package com.lifeforce.payer.request.domain;

import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.RequestedService;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
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

    public AuthorizationRequest build(HttpAuthorizationRequest httpAuthorizationRequest) {
        this.id = httpAuthorizationRequest.requestId();
        this.patientId = httpAuthorizationRequest.patientId();
        this.providerId = httpAuthorizationRequest.providerId();
        this.organizationId = httpAuthorizationRequest.organizationId();
        this.planId = httpAuthorizationRequest.planId();
        updateStatusToSubmitted();
        this.requestedService = httpAuthorizationRequest.requestedService();
        this.clinicalJustification = httpAuthorizationRequest.clinicalJustification();
        this.submittedAt = httpAuthorizationRequest.submittedAt();
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

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
    Status status;

    @Column(name = "status_reason", nullable = false)
    @Enumerated(EnumType.STRING)
    StatusReason statusReason;

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
        this.status = Status.SUBMITTED;
        this.statusReason = null;
        this.requestedService = httpAuthorizationRequest.requestedService();
        this.clinicalJustification = httpAuthorizationRequest.clinicalJustification();
        this.submittedAt = httpAuthorizationRequest.submittedAt();
        return this;
    }
}

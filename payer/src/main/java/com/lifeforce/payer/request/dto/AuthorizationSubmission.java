package com.lifeforce.payer.request.dto;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AuthorizationSubmission(
    @NotNull UUID requestId,
    @NotNull Instant submittedAt,
    @NotNull UUID patientId,
    @NotNull UUID providerId,
    @NotNull UUID organizationId,
    @NotNull UUID planId,
    @Valid @NotNull RequestedService requestedService,
    @Valid @NotNull ClinicalJustification clinicalJustification
) {
    public AuthorizationRequest toDomain() {
        return new AuthorizationRequest().build(
                requestId, submittedAt, patientId, providerId, organizationId, planId,
                requestedService.toDomain(), clinicalJustification.toDomain()
        );
    }
}


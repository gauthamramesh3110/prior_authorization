package com.lifeforce.payer.request.dto;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record HttpAuthorizationRequest(
    @Valid @NotNull UUID requestId,
    @Valid @NotNull Instant submittedAt,
    @Valid @NotNull UUID patientId,
    @Valid @NotNull UUID providerId,
    @Valid @NotNull UUID organizationId,
    @Valid @NotNull UUID planId,
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


package com.lifeforce.payer.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EvidenceRequest(
        @NotNull UUID reviewerId,
        @Valid @NotNull RequestedEvidence evidenceRequest
) {}

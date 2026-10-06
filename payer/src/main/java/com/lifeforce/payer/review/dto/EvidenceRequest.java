package com.lifeforce.payer.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record EvidenceRequest(
        @NotNull UUID reviewerId,
        @NotBlank String message,
        @NotEmpty List<@NotBlank String> requestedEvidence
) {}

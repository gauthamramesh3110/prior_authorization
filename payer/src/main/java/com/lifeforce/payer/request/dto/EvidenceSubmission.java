package com.lifeforce.payer.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EvidenceSubmission(
        @NotNull UUID providerId,
        @Valid @NotNull ClinicalJustification clinicalJustification
) {}

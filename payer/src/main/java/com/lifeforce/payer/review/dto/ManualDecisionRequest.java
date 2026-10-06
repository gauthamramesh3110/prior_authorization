package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.review.domain.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ManualDecisionRequest(
        @NotNull UUID reviewerId,
        @NotNull Decision decision,
        @NotBlank String decisionReason,
        @Positive Integer approvedQuantity
) {}

package com.lifeforce.payer.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public record ClinicalJustification(
        String summary,
        List<@Valid ConditionEvidence> conditions,
        List<@Valid ObservationEvidence> observations
) {
    public record ConditionEvidence(
        @NotBlank String code,
        String description,
        @NotNull Date startDate,
        Date endDate
    ) {}

    public record ObservationEvidence(
        @NotBlank String code,
        @NotNull BigDecimal value,
        @NotNull String units,
        String description,
        @NotNull Instant recordedAt
    ) {}
}

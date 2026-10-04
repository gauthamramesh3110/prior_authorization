package com.lifeforce.payer.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public record ClinicalJustification(
        String summary,
        List<ConditionEvidence> conditions,
        List<ObservationEvidence> observations
) {
    record ConditionEvidence(
        @NotBlank String code,
        String description,
        @NotNull Date startDate,
        Date endDate
    ) {}

    record ObservationEvidence(
        @NotBlank String code,
        @NotNull Integer value,
        @NotNull String units,
        String description,
        @NotNull Instant recordedAt
    ) {}
}

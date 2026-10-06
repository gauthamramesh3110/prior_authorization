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
        List<@NotNull @Valid ConditionEvidence> conditions,
        List<@NotNull @Valid ObservationEvidence> observations
) {
    public com.lifeforce.payer.request.domain.ClinicalJustification toDomain() {
        return new com.lifeforce.payer.request.domain.ClinicalJustification(
                summary,
                conditions == null ? null : conditions.stream().map(ConditionEvidence::toDomain).toList(),
                observations == null ? null : observations.stream().map(ObservationEvidence::toDomain).toList()
        );
    }

    public static ClinicalJustification from(com.lifeforce.payer.request.domain.ClinicalJustification justification) {
        if (justification == null) {
            return null;
        }
        return new ClinicalJustification(
                justification.summary(),
                justification.conditions() == null ? null : justification.conditions().stream().map(ConditionEvidence::from).toList(),
                justification.observations() == null ? null : justification.observations().stream().map(ObservationEvidence::from).toList()
        );
    }

    public record ConditionEvidence(
        @NotBlank String code,
        String description,
        @NotNull Date startDate,
        Date endDate
    ) {
        public com.lifeforce.payer.request.domain.ClinicalJustification.ConditionEvidence toDomain() {
            return new com.lifeforce.payer.request.domain.ClinicalJustification.ConditionEvidence(code, description, startDate, endDate);
        }

        public static ConditionEvidence from(com.lifeforce.payer.request.domain.ClinicalJustification.ConditionEvidence condition) {
            return new ConditionEvidence(condition.code(), condition.description(), condition.startDate(), condition.endDate());
        }
    }

    public record ObservationEvidence(
        @NotBlank String code,
        @NotNull BigDecimal value,
        @NotNull String units,
        String description,
        @NotNull Instant recordedAt
    ) {
        public com.lifeforce.payer.request.domain.ClinicalJustification.ObservationEvidence toDomain() {
            return new com.lifeforce.payer.request.domain.ClinicalJustification.ObservationEvidence(code, value, units, description, recordedAt);
        }

        public static ObservationEvidence from(com.lifeforce.payer.request.domain.ClinicalJustification.ObservationEvidence observation) {
            return new ObservationEvidence(observation.code(), observation.value(), observation.units(), observation.description(), observation.recordedAt());
        }
    }
}

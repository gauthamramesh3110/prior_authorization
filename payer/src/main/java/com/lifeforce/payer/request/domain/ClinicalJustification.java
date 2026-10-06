package com.lifeforce.payer.request.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public record ClinicalJustification(
        String summary,
        List<ConditionEvidence> conditions,
        List<ObservationEvidence> observations
) {
    public record ConditionEvidence(
            String code,
            String description,
            Date startDate,
            Date endDate
    ) {}

    public record ObservationEvidence(
            String code,
            BigDecimal value,
            String units,
            String description,
            Instant recordedAt
    ) {}
}

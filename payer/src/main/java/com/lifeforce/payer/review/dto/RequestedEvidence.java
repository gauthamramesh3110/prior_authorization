package com.lifeforce.payer.review.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RequestedEvidence(
        @NotBlank String summary,
        List<@NotBlank String> requestedConditions,
        List<@NotBlank String> requestedObservations,
        String otherEvidence
) {
    public com.lifeforce.payer.review.domain.RequestedEvidence toDomain() {
        return new com.lifeforce.payer.review.domain.RequestedEvidence(
                summary, requestedConditions, requestedObservations, otherEvidence
        );
    }

    public static RequestedEvidence from(com.lifeforce.payer.review.domain.RequestedEvidence evidence) {
        if (evidence == null) {
            return null;
        }
        return new RequestedEvidence(
                evidence.summary(), evidence.requestedConditions(), evidence.requestedObservations(), evidence.otherEvidence()
        );
    }
}

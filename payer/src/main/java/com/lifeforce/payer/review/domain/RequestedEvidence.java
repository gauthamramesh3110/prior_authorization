package com.lifeforce.payer.review.domain;

import java.util.List;

public record RequestedEvidence(
        String summary,
        List<String> requestedConditions,
        List<String> requestedObservations,
        String otherEvidence
) {
    public RequestedEvidence {
        requestedConditions = requestedConditions == null ? List.of() : List.copyOf(requestedConditions);
        requestedObservations = requestedObservations == null ? List.of() : List.copyOf(requestedObservations);
    }
}

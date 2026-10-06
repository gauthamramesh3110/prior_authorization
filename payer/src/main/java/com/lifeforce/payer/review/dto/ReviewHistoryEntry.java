package com.lifeforce.payer.review.dto;

import com.lifeforce.payer.review.domain.ReviewHistory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ReviewHistoryEntry(
        UUID id,
        UUID reviewId,
        String eventSource,
        String eventType,
        Instant eventAt,
        Map<String, Object> eventPayload
) {
    public static ReviewHistoryEntry from(ReviewHistory history) {
        return new ReviewHistoryEntry(
                history.getId(), history.getReviewId(), history.getEventSource(), history.getEventType(),
                history.getEventAt(), new LinkedHashMap<>(history.getEventPayload())
        );
    }
}

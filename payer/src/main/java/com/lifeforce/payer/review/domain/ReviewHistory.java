package com.lifeforce.payer.review.domain;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Entity(name = "review_history")
public class ReviewHistory {
    @Id
    private UUID id;

    private UUID reviewId;

    private String eventSource;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> eventPayload;

    private String eventType;

    private Instant eventAt;

    public static ReviewHistory createReviewCreatedEvent(Review review, AuthorizationRequest request) {
        ReviewHistory history = createEvent(review, request, "SYSTEM");
        history.eventType = "REVIEW_CREATED";
        return history;
    }

    public static ReviewHistory createSystemEvent(Review review) {
        return createEvent(review, review.getAuthorizationRequest(), "SYSTEM");
    }

    public static ReviewHistory createReviewerEvent(Review review) {
        return createEvent(review, review.getAuthorizationRequest(), "REVIEWER");
    }

    public static ReviewHistory createEvidenceRequestedEvent(Review review, String message, List<String> requestedEvidence) {
        ReviewHistory history = createReviewerEvent(review);
        history.eventType = "EVIDENCE_REQUESTED";
        history.eventPayload.put("message", message);
        history.eventPayload.put("requestedEvidence", List.copyOf(requestedEvidence));
        return history;
    }

    private static ReviewHistory createEvent(Review review, AuthorizationRequest request, String eventSource) {
        ReviewHistory history = new ReviewHistory();
        history.id = UUID.randomUUID();
        history.reviewId = review.getId();
        history.eventSource = eventSource;
        history.eventType = request.getRequestStatusReason().name();
        history.eventAt = review.getLastUpdated();
        history.eventPayload = new LinkedHashMap<>();
        history.eventPayload.put("reviewStatus", review.getReviewStatus());
        history.eventPayload.put("requestStatus", request.getRequestStatus());
        history.eventPayload.put("statusReason", request.getRequestStatusReason());
        history.eventPayload.put("decision", review.getDecision());
        history.eventPayload.put("approvedQuantity", review.getApprovedQuantity());
        history.eventPayload.put("decisionReason", review.getDecisionReason());
        history.eventPayload.put("decisionDate", review.getDecisionDate());
        history.eventPayload.put("decidedBy", review.getDecidedBy());
        history.eventPayload.put("reviewerId", review.getReviewerId());
        history.eventPayload.put("validFrom", review.getValidFrom());
        history.eventPayload.put("validTo", review.getValidTo());
        return history;
    }
}

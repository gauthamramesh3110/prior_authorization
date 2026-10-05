package com.lifeforce.payer.review.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity(name = "review")
public class Review {
    @Id
    UUID id;

    UUID requestId;

    @Enumerated(EnumType.STRING)
    Status status;

    @Enumerated(EnumType.STRING)
    Decision decision;

    String decisionReason;

    Instant decisionDate;

    @Enumerated(EnumType.STRING)
    DecisionActor decidedBy;

    Instant lastUpdated;

    UUID reviewerId;

    Instant validFrom;

    Instant validTo;

    Integer approvedQuantity;

    public static Review createNewReview(
            UUID requestId,
            Clock clock
    ) {
        Review review = new Review();
        review.id = UUID.randomUUID();
        review.requestId = requestId;
        review.status = Status.PENDING_EVALUATION;
        review.lastUpdated = Instant.now(clock);
        return review;
    }

    public static Review createNewManualReview(
            UUID requestId,
            Clock clock
    ) {
        Review review = new Review();
        review.id = UUID.randomUUID();
        review.requestId = requestId;
        review.status = Status.PENDING_MANUAL_REVIEW;
        review.lastUpdated = Instant.now(clock);
        return review;
    }
}

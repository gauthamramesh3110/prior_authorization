package com.lifeforce.payer.review.domain;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Entity(name = "review")
public class Review {
    @Id
    UUID id;

    @Column(name = "request_id")
    UUID requestId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    ReviewStatus reviewStatus;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false, nullable = false)
    AuthorizationRequest authorizationRequest;

    public static Review createNewReview(
            UUID requestId,
            Clock clock
    ) {
        Review review = new Review();
        review.id = UUID.randomUUID();
        review.requestId = requestId;
        review.reviewStatus = ReviewStatus.PENDING_EVALUATION;
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
        review.reviewStatus = ReviewStatus.PENDING_MANUAL_REVIEW;
        review.lastUpdated = Instant.now(clock);
        return review;
    }

    public void updateStatusToManualReview(Clock clock) {
        this.reviewStatus = ReviewStatus.PENDING_MANUAL_REVIEW;
        this.lastUpdated = Instant.now(clock);
    }

    public void updateStatusToAwaitingEvidence(Clock clock) {
        this.reviewStatus = ReviewStatus.AWAITING_EVIDENCE;
        this.lastUpdated = Instant.now(clock);
    }

    public void updateStatusToAutoApproved(Integer quantity, Clock clock) {
        Instant decisionTime = Instant.now(clock);
        this.reviewStatus = ReviewStatus.DECIDED;
        this.lastUpdated = decisionTime;
        this.decision = Decision.APPROVED;
        this.decisionReason = "Auto Approved";
        this.decisionDate = decisionTime;
        this.approvedQuantity = quantity;
        this.decidedBy = DecisionActor.SYSTEM;
        this.validFrom = decisionTime;
        this.validTo = decisionTime.plus(30, ChronoUnit.DAYS);
    }
}

package com.lifeforce.payer.review;

import com.lifeforce.payer.plan.repository.PlanServiceRepository;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.RequestedEvidence;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewHistoryQueryServiceTests {
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;
    @Mock PlanServiceRepository planServiceRepository;

    ReviewQueryService reviewQueryService;
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        reviewQueryService = new ReviewQueryService(reviewRepository, planServiceRepository, reviewHistoryRepository);
    }

    @AfterEach
    void verifiesHistoryQueriesDoNotWriteOrReadPolicies() {
        verify(reviewRepository, never()).save(any());
        verify(reviewHistoryRepository, never()).save(any());
        verifyNoInteractions(planServiceRepository);
    }

    @Test
    void returnsMissingForUnknownReviewWithoutQueryingHistory() {
        assertTrue(reviewQueryService.getReviewHistory(UUID.randomUUID()).isEmpty());
        verifyNoInteractions(reviewHistoryRepository);
    }

    @Test
    void returnsEmptyHistoryForExistingReview() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.existsById(reviewId)).thenReturn(true);

        assertEquals(List.of(), reviewQueryService.getReviewHistory(reviewId).orElseThrow());
        verify(reviewHistoryRepository).findByReviewIdOrderByEventAtAscIdAsc(reviewId);
    }

    @Test
    void returnsStoredHistoryInRepositoryOrderWithOriginalEvidenceAndDecisionSnapshots() {
        AuthorizationRequest request = new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", null, null, Date.from(submittedAt), 5), new ClinicalJustification("Original evidence", null, null)
        );
        request.updateStatusToManualReview();
        Review review = Review.createNewManualReview(request.getId(), clock);
        ReflectionTestUtils.setField(review, "authorizationRequest", request);
        ReviewHistory created = ReviewHistory.createReviewCreatedEvent(review, request);
        UUID reviewerId = UUID.randomUUID();
        review.requestEvidence(reviewerId, new RequestedEvidence("Please provide a report", List.of(), List.of(), "Report"), Clock.fixed(clock.instant().plusSeconds(1), ZoneOffset.UTC));
        request.updateStatusToAwaitingEvidence();
        ReviewHistory requested = ReviewHistory.createEvidenceRequestedEvent(review);
        ClinicalJustification additions = new ClinicalJustification("New evidence", null, null);
        Clock evidenceClock = Clock.fixed(clock.instant().plusSeconds(2), ZoneOffset.UTC);
        request.addEvidence(additions, evidenceClock);
        review.updateStatusToManualReview(evidenceClock);
        ReviewHistory submitted = ReviewHistory.createEvidenceSubmittedEvent(review, request.getProviderId(), additions);
        request.updateStatusToManuallyApproved();
        review.updateStatusToManuallyDecided(Decision.APPROVED, "Evidence supports approval", reviewerId, 3,
                Clock.fixed(clock.instant().plusSeconds(3), ZoneOffset.UTC));
        ReviewHistory decided = ReviewHistory.createReviewerEvent(review);
        List<ReviewHistory> histories = List.of(created, requested, submitted, decided);
        when(reviewRepository.existsById(review.getId())).thenReturn(true);
        when(reviewHistoryRepository.findByReviewIdOrderByEventAtAscIdAsc(review.getId())).thenReturn(histories);

        var history = reviewQueryService.getReviewHistory(review.getId()).orElseThrow();

        assertEquals(histories.stream().map(ReviewHistory::getId).toList(), history.stream().map(entry -> entry.id()).toList());
        assertEquals(List.of("REVIEW_CREATED", "EVIDENCE_REQUESTED", "UPDATED_EVIDENCE", "MANUAL_APPROVED"), history.stream().map(entry -> entry.eventType()).toList());
        assertEquals(List.of("SYSTEM", "REVIEWER", "PROVIDER", "REVIEWER"), history.stream().map(entry -> entry.eventSource()).toList());
        assertEquals(clock.instant(), history.getFirst().eventAt());
        assertEquals(review.getId(), history.getFirst().reviewId());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, history.getFirst().eventPayload().get("reviewStatus"));
        assertNull(history.getFirst().eventPayload().get("decision"));
        assertEquals(RequestStatusReason.AWAITING_EVIDENCE, history.get(1).eventPayload().get("statusReason"));
        assertEquals(new RequestedEvidence("Please provide a report", List.of(), List.of(), "Report"), history.get(1).eventPayload().get("evidenceRequest"));
        assertEquals(additions, history.get(2).eventPayload().get("clinicalJustification"));
        assertEquals(request.getProviderId(), history.get(2).eventPayload().get("providerId"));
        assertEquals(Decision.APPROVED, history.getLast().eventPayload().get("decision"));
        assertEquals(3, history.getLast().eventPayload().get("approvedQuantity"));
        assertEquals("Evidence supports approval", history.getLast().eventPayload().get("decisionReason"));
        assertEquals(submittedAt, request.getSubmittedAt());
        assertEquals(ReviewStatus.DECIDED, review.getReviewStatus());
        verify(reviewRepository, never()).findById(any());
        verify(reviewHistoryRepository).findByReviewIdOrderByEventAtAscIdAsc(review.getId());
    }
}

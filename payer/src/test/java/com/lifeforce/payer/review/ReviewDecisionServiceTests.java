package com.lifeforce.payer.review;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ManualDecisionRequest;
import com.lifeforce.payer.review.dto.ReviewDecisionResponse;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.service.ReviewDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewDecisionServiceTests {
    @Mock ReviewRepository reviewRepository;
    @Mock AuthorizationRequestRepository authorizationRequestRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;

    ReviewDecisionService reviewDecisionService;
    UUID reviewerId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        reviewDecisionService = new ReviewDecisionService(reviewRepository, authorizationRequestRepository, reviewHistoryRepository, clock);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5})
    void approvesUpToRequestedQuantityAndRecordsReviewer(Integer quantity) {
        Review review = givenManualReview();

        ReviewDecisionResponse response = reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, quantity));

        assertEquals(ReviewStatus.DECIDED, review.getReviewStatus());
        assertEquals(RequestStatus.APPROVED, review.getAuthorizationRequest().getRequestStatus());
        assertEquals(RequestStatusReason.MANUAL_APPROVED, review.getAuthorizationRequest().getRequestStatusReason());
        assertEquals(Decision.APPROVED, review.getDecision());
        assertEquals(quantity, review.getApprovedQuantity());
        assertEquals(DecisionActor.REVIEWER, review.getDecidedBy());
        assertEquals(reviewerId, review.getReviewerId());
        assertEquals("Clinical review completed", review.getDecisionReason());
        assertEquals(clock.instant(), review.getDecisionDate());
        assertEquals(clock.instant(), review.getLastUpdated());
        assertEquals(clock.instant(), review.getValidFrom());
        assertEquals(clock.instant().plus(30, ChronoUnit.DAYS), review.getValidTo());
        assertResponseMatchesReview(response, review);
        assertSavedReviewAndHistory(review);
    }

    @Test
    void rejectsReviewWithoutQuantityOrValidityDates() {
        Review review = givenManualReview();

        ReviewDecisionResponse response = reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.REJECTED, null));

        assertEquals(ReviewStatus.DECIDED, review.getReviewStatus());
        assertEquals(RequestStatus.REJECTED, review.getAuthorizationRequest().getRequestStatus());
        assertEquals(RequestStatusReason.MANUAL_REJECTED, review.getAuthorizationRequest().getRequestStatusReason());
        assertEquals(Decision.REJECTED, review.getDecision());
        assertEquals(DecisionActor.REVIEWER, review.getDecidedBy());
        assertEquals(reviewerId, review.getReviewerId());
        assertEquals("Clinical review completed", review.getDecisionReason());
        assertEquals(clock.instant(), review.getDecisionDate());
        assertEquals(clock.instant(), review.getLastUpdated());
        assertNull(review.getApprovedQuantity());
        assertNull(review.getValidFrom());
        assertNull(review.getValidTo());
        assertResponseMatchesReview(response, review);
        assertSavedReviewAndHistory(review);
    }

    @Test
    void returnsNotFoundForMissingReview() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(UUID.randomUUID(), decision(Decision.APPROVED, 5))
        );

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(authorizationRequestRepository, reviewHistoryRepository);
    }

    @ParameterizedTest
    @EnumSource(value = ReviewStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING_MANUAL_REVIEW")
    void refusesReviewsOutsideManualQueue(ReviewStatus status) {
        Review review = givenManualReview();
        ReflectionTestUtils.setField(review, "reviewStatus", status);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, 5))
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(status, review.getReviewStatus());
        assertEquals(RequestStatus.PENDING, review.getAuthorizationRequest().getRequestStatus());
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void refusesRequestThatIsNoLongerPending(RequestStatus status) {
        Review review = givenManualReview();
        review.getAuthorizationRequest().updateStatus(status, RequestStatusReason.MANUAL_REVIEW_REQUIRED);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.REJECTED, null))
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(status, review.getAuthorizationRequest().getRequestStatus());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, review.getReviewStatus());
        assertNoWrites();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, 6})
    void rejectsInvalidApprovalQuantityBeforeChangingState(Integer quantity) {
        Review review = givenManualReview();

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, quantity))
        );

        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
        assertUnchangedManualReview(review);
        assertNoWrites();
    }

    @Test
    void rejectsQuantityOnRejectedDecisionBeforeChangingState() {
        Review review = givenManualReview();

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.REJECTED, 5))
        );

        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
        assertUnchangedManualReview(review);
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(Decision.class)
    void refusesSecondDecisionAndPreservesOriginalHistory(Decision firstDecision) {
        Review review = givenManualReview();
        Integer quantity = firstDecision == Decision.APPROVED ? 5 : null;
        reviewDecisionService.submitManualDecision(review.getId(), decision(firstDecision, quantity));
        Decision secondDecision = firstDecision == Decision.APPROVED ? Decision.REJECTED : Decision.APPROVED;

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(secondDecision, secondDecision == Decision.APPROVED ? 5 : null))
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(firstDecision, review.getDecision());
        assertEquals(reviewerId, review.getReviewerId());
        assertSavedReviewAndHistory(review);
    }

    @Test
    void propagatesRequestSaveFailureWithoutSavingReviewOrHistory() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("Request unavailable");
        doThrow(failure).when(authorizationRequestRepository).save(review.getAuthorizationRequest());

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, 5))
        ));
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(reviewHistoryRepository);
    }

    @Test
    void propagatesReviewSaveFailureWithoutSavingHistory() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("Review unavailable");
        doThrow(failure).when(reviewRepository).save(review);

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, 5))
        ));
        verifyNoInteractions(reviewHistoryRepository);
    }

    @Test
    void propagatesHistorySaveFailure() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("History unavailable");
        doThrow(failure).when(reviewHistoryRepository).save(any());

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.REJECTED, null))
        ));
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
    }

    Review givenManualReview() {
        AuthorizationRequest request = new AuthorizationRequest().build(new HttpAuthorizationRequest(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", null, null, Date.from(submittedAt), 5),
                new ClinicalJustification(null, List.of(), List.of())
        ));
        request.updateStatus(RequestStatus.PENDING, RequestStatusReason.CRITERIA_NOT_MET);
        Review review = Review.createNewManualReview(request.getId(), Clock.fixed(submittedAt, ZoneOffset.UTC));
        ReflectionTestUtils.setField(review, "authorizationRequest", request);
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        return review;
    }

    ManualDecisionRequest decision(Decision decision, Integer quantity) {
        return new ManualDecisionRequest(reviewerId, decision, "Clinical review completed", quantity);
    }

    void assertUnchangedManualReview(Review review) {
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, review.getReviewStatus());
        assertEquals(RequestStatus.PENDING, review.getAuthorizationRequest().getRequestStatus());
        assertEquals(RequestStatusReason.CRITERIA_NOT_MET, review.getAuthorizationRequest().getRequestStatusReason());
        assertEquals(submittedAt, review.getLastUpdated());
        assertNull(review.getDecision());
        assertNull(review.getReviewerId());
    }

    void assertNoWrites() {
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(authorizationRequestRepository, reviewHistoryRepository);
    }

    void assertResponseMatchesReview(ReviewDecisionResponse response, Review review) {
        assertEquals(review.getId(), response.id());
        assertEquals(review.getRequestId(), response.requestId());
        assertEquals(ReviewStatus.DECIDED, response.reviewStatus());
        assertEquals(review.getAuthorizationRequest().getRequestStatus(), response.requestStatus());
        assertEquals(review.getAuthorizationRequest().getRequestStatusReason(), response.requestStatusReason());
        assertEquals(review.getDecision(), response.decision());
        assertEquals("Clinical review completed", response.decisionReason());
        assertEquals(clock.instant(), response.decisionDate());
        assertEquals(DecisionActor.REVIEWER, response.decidedBy());
        assertEquals(reviewerId, response.reviewerId());
        assertEquals(review.getApprovedQuantity(), response.approvedQuantity());
        assertEquals(review.getValidFrom(), response.validFrom());
        assertEquals(review.getValidTo(), response.validTo());
    }

    void assertSavedReviewAndHistory(Review review) {
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
        ArgumentCaptor<ReviewHistory> savedHistory = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewHistoryRepository).save(savedHistory.capture());
        ReviewHistory history = savedHistory.getValue();
        assertEquals(review.getId(), history.getReviewId());
        assertEquals("REVIEWER", history.getEventSource());
        assertEquals(review.getAuthorizationRequest().getRequestStatusReason().name(), history.getEventType());
        assertEquals(clock.instant(), history.getEventAt());
        assertEquals(ReviewStatus.DECIDED, history.getEventPayload().get("reviewStatus"));
        assertEquals(review.getAuthorizationRequest().getRequestStatus(), history.getEventPayload().get("requestStatus"));
        assertEquals(review.getAuthorizationRequest().getRequestStatusReason(), history.getEventPayload().get("statusReason"));
        assertEquals(review.getDecision(), history.getEventPayload().get("decision"));
        assertEquals("Clinical review completed", history.getEventPayload().get("decisionReason"));
        assertEquals(clock.instant(), history.getEventPayload().get("decisionDate"));
        assertEquals(DecisionActor.REVIEWER, history.getEventPayload().get("decidedBy"));
        assertEquals(reviewerId, history.getEventPayload().get("reviewerId"));
        assertEquals(review.getApprovedQuantity(), history.getEventPayload().get("approvedQuantity"));
        assertEquals(review.getValidFrom(), history.getEventPayload().get("validFrom"));
        assertEquals(review.getValidTo(), history.getEventPayload().get("validTo"));
    }
}

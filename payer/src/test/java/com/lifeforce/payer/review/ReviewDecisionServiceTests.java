package com.lifeforce.payer.review;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ManualDecisionRequest;
import com.lifeforce.payer.review.dto.ReviewDecisionResponse;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.service.ReviewDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    ReviewDecisionService reviewDecisionService;
    UUID reviewerId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        reviewDecisionService = new ReviewDecisionService(reviewRepository, authorizationRequestRepository, clock);
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
        assertSavedRequestAndReview(review);
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
        assertSavedRequestAndReview(review);
    }

    @Test
    void returnsNotFoundForMissingReview() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewDecisionService.submitManualDecision(UUID.randomUUID(), decision(Decision.APPROVED, 5))
        );

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(authorizationRequestRepository);
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
        switch (status) {
            case SUBMITTED -> review.getAuthorizationRequest().updateStatusToSubmitted();
            case APPROVED -> review.getAuthorizationRequest().updateStatusToManuallyApproved();
            case REJECTED -> review.getAuthorizationRequest().updateStatusToManuallyRejected();
            default -> throw new IllegalArgumentException("Expected a non-pending request status");
        }

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
    void refusesSecondDecisionAndPreservesOriginalDecision(Decision firstDecision) {
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
        assertSavedRequestAndReview(review);
    }

    @Test
    void propagatesRequestSaveFailureWithoutSavingReview() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("Request unavailable");
        doThrow(failure).when(authorizationRequestRepository).save(review.getAuthorizationRequest());

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, 5))
        ));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void propagatesReviewSaveFailure() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("Review unavailable");
        doThrow(failure).when(reviewRepository).save(review);

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                reviewDecisionService.submitManualDecision(review.getId(), decision(Decision.APPROVED, 5))
        ));
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
    }

    Review givenManualReview() {
        AuthorizationRequest request = new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", null, null, Date.from(submittedAt), 5),
                new ClinicalJustification(null, List.of(), List.of())
        );
        request.updateStatusToCriteriaNotMet();
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
        verifyNoInteractions(authorizationRequestRepository);
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

    void assertSavedRequestAndReview(Review review) {
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
    }
}

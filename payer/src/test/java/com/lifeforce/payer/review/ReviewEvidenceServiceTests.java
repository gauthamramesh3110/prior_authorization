package com.lifeforce.payer.review;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.EvidenceRequest;
import com.lifeforce.payer.review.dto.EvidenceRequestResponse;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.service.ReviewEvidenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEvidenceServiceTests {
    @Mock ReviewRepository reviewRepository;
    @Mock AuthorizationRequestRepository authorizationRequestRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;

    ReviewEvidenceService reviewEvidenceService;
    UUID reviewerId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        reviewEvidenceService = new ReviewEvidenceService(reviewRepository, authorizationRequestRepository, reviewHistoryRepository, clock);
    }

    @ParameterizedTest
    @EnumSource(value = ReviewStatus.class, names = {"PENDING_MANUAL_REVIEW", "AWAITING_EVIDENCE"})
    void requestsEvidenceForManualAndAwaitingReviews(ReviewStatus initialStatus) {
        Review review = givenManualReview();
        if (initialStatus == ReviewStatus.AWAITING_EVIDENCE) {
            review.updateStatusToAwaitingEvidence(Clock.fixed(submittedAt, ZoneOffset.UTC));
            review.getAuthorizationRequest().updateStatusToAwaitingEvidence();
        }
        ClinicalJustification originalEvidence = review.getAuthorizationRequest().getClinicalJustification();

        EvidenceRequestResponse response = reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest());

        assertEquals(ReviewStatus.AWAITING_EVIDENCE, review.getReviewStatus());
        assertEquals(RequestStatus.PENDING, review.getAuthorizationRequest().getRequestStatus());
        assertEquals(RequestStatusReason.AWAITING_EVIDENCE, review.getAuthorizationRequest().getRequestStatusReason());
        assertEquals(reviewerId, review.getReviewerId());
        assertEquals(clock.instant(), review.getLastUpdated());
        assertEquals(submittedAt, review.getAuthorizationRequest().getSubmittedAt());
        assertEquals(originalEvidence, review.getAuthorizationRequest().getClinicalJustification());
        assertNull(review.getDecision());
        assertNull(review.getDecisionReason());
        assertNull(review.getDecisionDate());
        assertNull(review.getDecidedBy());
        assertNull(review.getApprovedQuantity());
        assertNull(review.getValidFrom());
        assertNull(review.getValidTo());
        assertEquals(review.getId(), response.reviewId());
        assertEquals(review.getRequestId(), response.requestId());
        assertEquals(reviewerId, response.reviewerId());
        assertEquals("Please provide supporting clinical evidence", response.message());
        assertEquals(List.of("EF observation", "Echocardiogram report"), response.requestedEvidence());
        assertEquals(clock.instant(), response.requestedAt());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, response.reviewStatus());
        assertEquals(RequestStatus.PENDING, response.requestStatus());
        assertEquals(RequestStatusReason.AWAITING_EVIDENCE, response.requestStatusReason());
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
        ReviewHistory history = capturedHistory();
        assertEquals(history.getId(), response.id());
        assertEquals(review.getId(), history.getReviewId());
        assertEquals("REVIEWER", history.getEventSource());
        assertEquals("EVIDENCE_REQUESTED", history.getEventType());
        assertEquals(clock.instant(), history.getEventAt());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, history.getEventPayload().get("reviewStatus"));
        assertEquals(RequestStatus.PENDING, history.getEventPayload().get("requestStatus"));
        assertEquals(RequestStatusReason.AWAITING_EVIDENCE, history.getEventPayload().get("statusReason"));
        assertEquals(reviewerId, history.getEventPayload().get("reviewerId"));
        assertEquals(response.message(), history.getEventPayload().get("message"));
        assertEquals(response.requestedEvidence(), history.getEventPayload().get("requestedEvidence"));
        assertNull(history.getEventPayload().get("decision"));
        assertNull(history.getEventPayload().get("decidedBy"));
    }

    @Test
    void returnsNotFoundForMissingReview() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewEvidenceService.requestEvidence(UUID.randomUUID(), evidenceRequest())
        );

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = ReviewStatus.class, names = {"PENDING_EVALUATION", "DECIDED"})
    void refusesEvaluatingAndCompletedReviews(ReviewStatus status) {
        Review review = givenManualReview();
        ReflectionTestUtils.setField(review, "reviewStatus", status);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest())
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(status, review.getReviewStatus());
        assertEquals(submittedAt, review.getLastUpdated());
        assertNull(review.getReviewerId());
        assertEquals(RequestStatusReason.MANUAL_REVIEW_REQUIRED, review.getAuthorizationRequest().getRequestStatusReason());
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void refusesRequestsThatAreNotPending(RequestStatus status) {
        Review review = givenManualReview();
        AuthorizationRequest request = review.getAuthorizationRequest();
        switch (status) {
            case SUBMITTED -> request.updateStatusToSubmitted();
            case APPROVED -> request.updateStatusToManuallyApproved();
            case REJECTED -> request.updateStatusToManuallyRejected();
            default -> throw new IllegalArgumentException("Expected a non-pending request status");
        }

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest())
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(status, request.getRequestStatus());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, review.getReviewStatus());
        assertNull(review.getReviewerId());
        assertNoWrites();
    }

    @Test
    void recordsEachAdditionalEvidenceRequestWithoutOverwritingEarlierHistory() {
        Review review = givenManualReview();
        EvidenceRequestResponse firstResponse = reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest());
        UUID secondReviewerId = UUID.randomUUID();
        EvidenceRequest secondRequest = new EvidenceRequest(secondReviewerId, "Please provide the latest report", List.of("Latest report"));

        EvidenceRequestResponse secondResponse = reviewEvidenceService.requestEvidence(review.getId(), secondRequest);

        ArgumentCaptor<ReviewHistory> histories = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewHistoryRepository, times(2)).save(histories.capture());
        assertNotEquals(firstResponse.id(), secondResponse.id());
        assertEquals(reviewerId, histories.getAllValues().getFirst().getEventPayload().get("reviewerId"));
        assertEquals(firstResponse.message(), histories.getAllValues().getFirst().getEventPayload().get("message"));
        assertEquals(secondReviewerId, histories.getAllValues().getLast().getEventPayload().get("reviewerId"));
        assertEquals(secondRequest.message(), histories.getAllValues().getLast().getEventPayload().get("message"));
        assertEquals(secondRequest.requestedEvidence(), histories.getAllValues().getLast().getEventPayload().get("requestedEvidence"));
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, review.getReviewStatus());
    }

    @Test
    void preservesRequestedItemsWhenCallerChangesItsList() {
        Review review = givenManualReview();
        List<String> requestedItems = new ArrayList<>(List.of("EF observation"));
        EvidenceRequest request = new EvidenceRequest(reviewerId, "Please provide an EF observation", requestedItems);

        EvidenceRequestResponse response = reviewEvidenceService.requestEvidence(review.getId(), request);
        requestedItems.add("Another item");

        assertEquals(List.of("EF observation"), response.requestedEvidence());
        assertEquals(List.of("EF observation"), capturedHistory().getEventPayload().get("requestedEvidence"));
    }

    @Test
    void propagatesRequestSaveFailureWithoutSavingReviewOrHistory() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("Request unavailable");
        doThrow(failure).when(authorizationRequestRepository).save(review.getAuthorizationRequest());

        assertSame(failure, assertThrows(IllegalStateException.class, () -> reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest())));
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(reviewHistoryRepository);
    }

    @Test
    void propagatesReviewSaveFailureWithoutSavingHistory() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("Review unavailable");
        doThrow(failure).when(reviewRepository).save(review);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest())));
        verifyNoInteractions(reviewHistoryRepository);
    }

    @Test
    void propagatesHistorySaveFailure() {
        Review review = givenManualReview();
        IllegalStateException failure = new IllegalStateException("History unavailable");
        doThrow(failure).when(reviewHistoryRepository).save(any());

        assertSame(failure, assertThrows(IllegalStateException.class, () -> reviewEvidenceService.requestEvidence(review.getId(), evidenceRequest())));
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
    }

    Review givenManualReview() {
        AuthorizationRequest request = new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", null, null, Date.from(submittedAt), 5),
                new ClinicalJustification("Submitted evidence", List.of(), List.of())
        );
        request.updateStatusToManualReview();
        Review review = Review.createNewManualReview(request.getId(), Clock.fixed(submittedAt, ZoneOffset.UTC));
        ReflectionTestUtils.setField(review, "authorizationRequest", request);
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        return review;
    }

    EvidenceRequest evidenceRequest() {
        return new EvidenceRequest(reviewerId, "Please provide supporting clinical evidence", List.of("EF observation", "Echocardiogram report"));
    }

    ReviewHistory capturedHistory() {
        ArgumentCaptor<ReviewHistory> history = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewHistoryRepository).save(history.capture());
        return history.getValue();
    }

    void assertNoWrites() {
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(authorizationRequestRepository, reviewHistoryRepository);
    }
}

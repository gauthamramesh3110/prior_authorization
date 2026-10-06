package com.lifeforce.payer.request;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.request.dto.EvidenceSubmission;
import com.lifeforce.payer.request.dto.EvidenceSubmissionResponse;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.service.EvidenceSubmissionService;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceSubmissionServiceTests {
    @Mock AuthorizationRequestRepository authorizationRequestRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;

    EvidenceSubmissionService evidenceSubmissionService;
    UUID providerId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        evidenceSubmissionService = new EvidenceSubmissionService(authorizationRequestRepository, reviewRepository, reviewHistoryRepository, clock);
    }

    @Test
    void appendsEvidenceAndQueuesAutomaticEvaluationWithoutChangingOriginalRequestFields() {
        Review review = givenAwaitingReview();
        AuthorizationRequest request = review.getAuthorizationRequest();
        var originalEvidence = request.getClinicalJustification();
        RequestedService originalService = request.getRequestedService();
        UUID patientId = request.getPatientId();
        UUID organizationId = request.getOrganizationId();
        UUID planId = request.getPlanId();
        ClinicalJustification additions = additionalEvidence();

        EvidenceSubmissionResponse response = evidenceSubmissionService.submitEvidence(request.getId(), new EvidenceSubmission(providerId, additions));

        assertEquals("Original evidence\n\nNew test results", request.getClinicalJustification().summary());
        assertEquals(List.of(originalEvidence.conditions().getFirst(), additions.conditions().getFirst().toDomain()), request.getClinicalJustification().conditions());
        assertEquals(List.of(originalEvidence.observations().getFirst(), additions.observations().getFirst().toDomain()), request.getClinicalJustification().observations());
        assertEquals(new BigDecimal("35.1"), request.getClinicalJustification().observations().getLast().value());
        assertEquals(submittedAt, request.getSubmittedAt());
        assertEquals(patientId, request.getPatientId());
        assertEquals(providerId, request.getProviderId());
        assertEquals(organizationId, request.getOrganizationId());
        assertEquals(planId, request.getPlanId());
        assertEquals(originalService, request.getRequestedService());
        assertEquals(clock.instant(), request.getEvidenceUpdatedAt());
        assertEquals(clock.instant(), request.getEvidenceEvaluationAt());
        assertEquals(ReviewStatus.PENDING_EVALUATION, review.getReviewStatus());
        assertEquals(RequestStatus.PENDING, request.getRequestStatus());
        assertEquals(RequestStatusReason.EVIDENCE_UPDATED, request.getRequestStatusReason());
        assertEquals(clock.instant(), review.getLastUpdated());
        assertNull(review.getDecision());
        verify(authorizationRequestRepository).save(request);
        verify(reviewRepository).save(review);
        ReviewHistory history = capturedHistory();
        assertEquals("PROVIDER", history.getEventSource());
        assertEquals("UPDATED_EVIDENCE", history.getEventType());
        assertEquals(review.getId(), history.getReviewId());
        assertEquals(clock.instant(), history.getEventAt());
        assertEquals(providerId, history.getEventPayload().get("providerId"));
        assertEquals(additions.toDomain(), history.getEventPayload().get("clinicalJustification"));
        assertEquals(ReviewStatus.PENDING_EVALUATION, history.getEventPayload().get("reviewStatus"));
        assertEquals(RequestStatusReason.EVIDENCE_UPDATED, history.getEventPayload().get("statusReason"));
        assertEquals(history.getId(), response.id());
        assertEquals(request.getId(), response.requestId());
        assertEquals(review.getId(), response.reviewId());
        assertEquals(providerId, response.providerId());
        assertEquals(review.getReviewStatus(), response.reviewStatus());
        assertEquals(request.getRequestStatus(), response.requestStatus());
        assertEquals(request.getRequestStatusReason(), response.requestStatusReason());
        assertEquals(clock.instant(), response.evidenceUpdatedAt());
    }

    @Test
    void returnsReviewerRequestedEvidenceToManualReview() {
        Review review = givenAwaitingReview();
        UUID reviewerId = UUID.randomUUID();
        review.requestEvidence(reviewerId, Clock.fixed(submittedAt, ZoneOffset.UTC));

        evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, additionalEvidence()));

        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, review.getReviewStatus());
        assertEquals(reviewerId, review.getReviewerId());
        assertNull(review.getDecision());
        assertNull(review.getDecidedBy());
        assertNull(review.getDecisionDate());
        assertNull(review.getApprovedQuantity());
        assertNull(review.getValidFrom());
        assertNull(review.getValidTo());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, capturedHistory().getEventPayload().get("reviewStatus"));
    }

    @Test
    void acceptsSummaryWithoutReplacingExistingStructuredEvidence() {
        Review review = givenAwaitingReview();
        var original = review.getAuthorizationRequest().getClinicalJustification();

        evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, new ClinicalJustification("Additional clinical context", null, null)));

        var merged = review.getAuthorizationRequest().getClinicalJustification();
        assertEquals("Original evidence\n\nAdditional clinical context", merged.summary());
        assertEquals(original.conditions(), merged.conditions());
        assertEquals(original.observations(), merged.observations());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptsConditionOrObservationWithoutSummary(boolean conditionSubmitted) {
        Review review = givenAwaitingReview();
        ClinicalJustification additions = additionalEvidence();
        ClinicalJustification submission = new ClinicalJustification(null,
                conditionSubmitted ? additions.conditions() : null,
                conditionSubmitted ? null : additions.observations());

        evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, submission));

        var merged = review.getAuthorizationRequest().getClinicalJustification();
        assertEquals("Original evidence", merged.summary());
        assertEquals(conditionSubmitted ? 2 : 1, merged.conditions().size());
        assertEquals(conditionSubmitted ? 1 : 2, merged.observations().size());
    }

    @Test
    void addsEvidenceWhenOriginalListsAreAbsent() {
        Review review = givenAwaitingReview();
        ReflectionTestUtils.setField(review.getAuthorizationRequest(), "clinicalJustification", new ClinicalJustification(null, null, null).toDomain());

        evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, additionalEvidence()));

        assertEquals(additionalEvidence().toDomain(), review.getAuthorizationRequest().getClinicalJustification());
    }

    @Test
    void returnsNotFoundForMissingRequest() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(UUID.randomUUID(), new EvidenceSubmission(providerId, additionalEvidence()))
        );

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        verifyNoInteractions(reviewRepository, reviewHistoryRepository);
        verify(authorizationRequestRepository, never()).save(any());
    }

    @Test
    void refusesEvidenceFromAnotherProviderBeforeFetchingReview() {
        AuthorizationRequest request = createRequest();
        when(authorizationRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(request.getId(), new EvidenceSubmission(UUID.randomUUID(), additionalEvidence()))
        );

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
        assertNull(request.getEvidenceUpdatedAt());
        verifyNoInteractions(reviewRepository, reviewHistoryRepository);
        verify(authorizationRequestRepository, never()).save(any());
    }

    @Test
    void refusesRequestWithoutReview() {
        AuthorizationRequest request = createRequest();
        when(authorizationRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(request.getId(), new EvidenceSubmission(providerId, additionalEvidence()))
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = ReviewStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "AWAITING_EVIDENCE")
    void refusesReviewsThatAreNotAwaitingEvidence(ReviewStatus status) {
        Review review = givenAwaitingReview();
        ReflectionTestUtils.setField(review, "reviewStatus", status);
        var original = review.getAuthorizationRequest().getClinicalJustification();

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, additionalEvidence()))
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(status, review.getReviewStatus());
        assertEquals(original, review.getAuthorizationRequest().getClinicalJustification());
        assertNull(review.getAuthorizationRequest().getEvidenceUpdatedAt());
        assertNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void refusesRequestsThatAreNotPending(RequestStatus status) {
        Review review = givenAwaitingReview();
        ReflectionTestUtils.setField(review.getAuthorizationRequest(), "requestStatus", status);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, additionalEvidence()))
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, review.getReviewStatus());
        assertNoWrites();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void refusesEmptyEvidenceWithoutChangingWorkflow(String summary) {
        Review review = givenAwaitingReview();
        var original = review.getAuthorizationRequest().getClinicalJustification();

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, new ClinicalJustification(summary, List.of(), List.of())))
        );

        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, review.getReviewStatus());
        assertEquals(original, review.getAuthorizationRequest().getClinicalJustification());
        assertNull(review.getAuthorizationRequest().getEvidenceUpdatedAt());
        assertNoWrites();
    }

    @Test
    void refusesRepeatSubmissionUntilEvidenceIsRequestedAgain() {
        Review review = givenAwaitingReview();
        EvidenceSubmission submission = new EvidenceSubmission(providerId, additionalEvidence());
        evidenceSubmissionService.submitEvidence(review.getRequestId(), submission);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () ->
                evidenceSubmissionService.submitEvidence(review.getRequestId(), submission)
        );

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        assertEquals(2, review.getAuthorizationRequest().getClinicalJustification().observations().size());
        verify(reviewHistoryRepository).save(any());
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
    }

    @Test
    void preservesEarlierEvidenceHistoryAcrossAnotherEvidenceRound() {
        Review review = givenAwaitingReview();
        ClinicalJustification firstAdditions = additionalEvidence();
        evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, firstAdditions));
        review.requestEvidence(UUID.randomUUID(), clock);
        review.getAuthorizationRequest().updateStatusToAwaitingEvidence();
        ClinicalJustification secondAdditions = new ClinicalJustification("Further clinical context", null, null);

        evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, secondAdditions));

        ArgumentCaptor<ReviewHistory> histories = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewHistoryRepository, times(2)).save(histories.capture());
        assertNotEquals(histories.getAllValues().getFirst().getId(), histories.getAllValues().getLast().getId());
        assertEquals(firstAdditions.toDomain(), histories.getAllValues().getFirst().getEventPayload().get("clinicalJustification"));
        assertEquals(secondAdditions.toDomain(), histories.getAllValues().getLast().getEventPayload().get("clinicalJustification"));
        assertEquals("Original evidence\n\nNew test results\n\nFurther clinical context", review.getAuthorizationRequest().getClinicalJustification().summary());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, review.getReviewStatus());
    }

    @Test
    void stopsSavingWhenRequestPersistenceFails() {
        Review review = givenAwaitingReview();
        IllegalStateException failure = new IllegalStateException("Request unavailable");
        doThrow(failure).when(authorizationRequestRepository).save(review.getAuthorizationRequest());

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                evidenceSubmissionService.submitEvidence(review.getRequestId(), new EvidenceSubmission(providerId, additionalEvidence()))
        ));
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(reviewHistoryRepository);
    }

    Review givenAwaitingReview() {
        AuthorizationRequest request = createRequest();
        request.updateStatusToAwaitingEvidence();
        Review review = Review.createNewReview(request.getId(), Clock.fixed(submittedAt, ZoneOffset.UTC));
        review.updateStatusToAwaitingEvidence(Clock.fixed(submittedAt, ZoneOffset.UTC));
        ReflectionTestUtils.setField(review, "authorizationRequest", request);
        when(authorizationRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(reviewRepository.findByRequestId(request.getId())).thenReturn(Optional.of(review));
        return review;
    }

    AuthorizationRequest createRequest() {
        return new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), providerId, UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", null, null, Date.from(submittedAt), 5),
                new ClinicalJustification("Original evidence",
                        List.of(new ClinicalJustification.ConditionEvidence("OLD", null, Date.from(submittedAt), null)),
                        List.of(new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("40"), "%", null, submittedAt))).toDomain()
        );
    }

    ClinicalJustification additionalEvidence() {
        return new ClinicalJustification("New test results",
                List.of(new ClinicalJustification.ConditionEvidence("NEW", null, Date.from(clock.instant()), null)),
                List.of(new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("35.1"), "%", null, clock.instant())));
    }

    ReviewHistory capturedHistory() {
        ArgumentCaptor<ReviewHistory> history = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewHistoryRepository).save(history.capture());
        return history.getValue();
    }

    void assertNoWrites() {
        verify(authorizationRequestRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(reviewHistoryRepository);
    }
}

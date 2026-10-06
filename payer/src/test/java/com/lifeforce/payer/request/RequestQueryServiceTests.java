package com.lifeforce.payer.request;

import com.lifeforce.payer.reference.repository.ProviderRepository;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.service.RequestQueryService;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.RequestedEvidence;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
class RequestQueryServiceTests {
    @Mock AuthorizationRequestRepository authorizationRequestRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ProviderRepository providerRepository;

    RequestQueryService requestQueryService;
    UUID providerId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        requestQueryService = new RequestQueryService(authorizationRequestRepository, reviewRepository, providerRepository);
    }

    @AfterEach
    void verifiesTrackingQueriesDoNotSaveRequestsOrReviews() {
        verify(authorizationRequestRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void returnsNotFoundForUnknownProviderWithoutReadingRequests() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () -> requestQueryService.getProviderRequests(providerId, null));

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        verifyNoInteractions(authorizationRequestRepository, reviewRepository);
    }

    @Test
    void returnsEmptyListForKnownProviderWithoutRequests() {
        when(providerRepository.existsById(providerId)).thenReturn(true);

        assertEquals(List.of(), requestQueryService.getProviderRequests(providerId, null));
        verify(authorizationRequestRepository).findByProviderIdOrderBySubmittedAtDescIdAsc(providerId);
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void returnsAllProviderRequestsInRepositoryOrderAndAssociatesReviewsByRequestId() {
        when(providerRepository.existsById(providerId)).thenReturn(true);
        AuthorizationRequest submitted = request();
        AuthorizationRequest awaiting = request();
        awaiting.updateStatusToAwaitingEvidence();
        Review awaitingReview = Review.createNewEvaluationReview(awaiting.getId(), clock);
        awaitingReview.updateStatusToAwaitingEvidence(clock);
        AuthorizationRequest approved = request();
        approved.updateStatusToAutoApproved();
        Review approvedReview = Review.createNewEvaluationReview(approved.getId(), clock);
        approvedReview.updateStatusToAutoApproved(5, clock);
        List<AuthorizationRequest> requests = List.of(submitted, awaiting, approved);
        List<UUID> requestIds = requests.stream().map(AuthorizationRequest::getId).toList();
        when(authorizationRequestRepository.findByProviderIdOrderBySubmittedAtDescIdAsc(providerId)).thenReturn(requests);
        when(reviewRepository.findByRequestIdIn(requestIds)).thenReturn(List.of(approvedReview, awaitingReview));

        var summaries = requestQueryService.getProviderRequests(providerId, null);

        assertEquals(requestIds, summaries.stream().map(summary -> summary.id()).toList());
        assertEquals(providerId, summaries.getFirst().providerId());
        assertEquals(submitted.getPatientId(), summaries.getFirst().patientId());
        assertEquals(submitted.getOrganizationId(), summaries.getFirst().organizationId());
        assertEquals(submitted.getPlanId(), summaries.getFirst().planId());
        assertEquals(submitted.getRequestedService(), summaries.getFirst().requestedService().toDomain());
        assertEquals(submittedAt, summaries.getFirst().submittedAt());
        assertEquals(RequestStatus.SUBMITTED, summaries.getFirst().requestStatus());
        assertNull(summaries.getFirst().requestStatusReason());
        assertNull(summaries.getFirst().reviewId());
        assertNull(summaries.getFirst().reviewStatus());
        assertEquals(RequestStatusReason.AWAITING_EVIDENCE, summaries.get(1).requestStatusReason());
        assertEquals(awaitingReview.getId(), summaries.get(1).reviewId());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, summaries.get(1).reviewStatus());
        assertEquals(approvedReview.getId(), summaries.getLast().reviewId());
        assertEquals(ReviewStatus.DECIDED, summaries.getLast().reviewStatus());
        verify(reviewRepository).findByRequestIdIn(requestIds);
        verify(reviewRepository, never()).findByRequestId(any());
        verify(authorizationRequestRepository, never()).findByProviderIdAndRequestStatusOrderBySubmittedAtDescIdAsc(any(), any());
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void scopesStatusFilterToTheSelectedProvider(RequestStatus status) {
        when(providerRepository.existsById(providerId)).thenReturn(true);

        assertTrue(requestQueryService.getProviderRequests(providerId, status).isEmpty());

        verify(authorizationRequestRepository).findByProviderIdAndRequestStatusOrderBySubmittedAtDescIdAsc(providerId, status);
        verify(authorizationRequestRepository, never()).findByProviderIdOrderBySubmittedAtDescIdAsc(any());
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void returnsNotFoundForMissingRequestDetails() {
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () -> requestQueryService.getProviderRequestDetails(UUID.randomUUID(), providerId));

        assertEquals(HttpStatus.NOT_FOUND, failure.getStatusCode());
        verifyNoInteractions(reviewRepository, providerRepository);
    }

    @Test
    void refusesAnotherProvidersRequestWithoutReadingReview() {
        AuthorizationRequest request = givenRequest();

        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () -> requestQueryService.getProviderRequestDetails(request.getId(), UUID.randomUUID()));

        assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
        verifyNoInteractions(reviewRepository, providerRepository);
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"SUBMITTED", "APPROVED", "REJECTED"})
    void returnsRequestDetailsWithoutRequiringAReview(RequestStatus status) {
        AuthorizationRequest request = givenRequest();
        if (status == RequestStatus.APPROVED) {
            request.updateStatusToPriorAuthNotRequired();
        } else if (status == RequestStatus.REJECTED) {
            request.updateStatusToCoverageInactive();
        }

        var details = requestQueryService.getProviderRequestDetails(request.getId(), providerId);

        assertEquals(request.getId(), details.request().id());
        assertEquals(status, details.request().requestStatus());
        assertEquals(request.getRequestStatusReason(), details.request().requestStatusReason());
        assertEquals(request.getRequestedService(), details.request().requestedService().toDomain());
        assertEquals(request.getClinicalJustification(), details.clinicalJustification().toDomain());
        assertEquals(submittedAt, details.request().submittedAt());
        assertNull(details.request().reviewId());
        assertNull(details.request().reviewStatus());
        assertNull(details.review());
        verify(reviewRepository).findByRequestId(request.getId());
    }

    @Test
    void returnsAwaitingEvidenceReviewWithoutADecision() {
        AuthorizationRequest request = givenRequest();
        request.updateStatusToAwaitingEvidence();
        Review review = Review.createNewManualReview(request.getId(), clock);
        UUID reviewerId = UUID.randomUUID();
        review.requestEvidence(reviewerId, new RequestedEvidence("Provide an EF result", List.of(), List.of("EF"), null), clock);
        when(reviewRepository.findByRequestId(request.getId())).thenReturn(Optional.of(review));

        var details = requestQueryService.getProviderRequestDetails(request.getId(), providerId);

        assertEquals(RequestStatusReason.AWAITING_EVIDENCE, details.request().requestStatusReason());
        assertEquals(review.getId(), details.review().id());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, details.review().reviewStatus());
        assertEquals(clock.instant(), details.review().lastUpdated());
        assertEquals(reviewerId, details.review().reviewerId());
        assertEquals(review.getEvidenceRequest(), details.review().evidenceRequest().toDomain());
        assertNull(details.review().decision());
        assertNull(details.review().approvedQuantity());
        assertNull(details.review().validFrom());
        assertNull(details.review().validTo());
        assertEquals(ReviewStatus.AWAITING_EVIDENCE, review.getReviewStatus());
    }

    @ParameterizedTest
    @EnumSource(Decision.class)
    void returnsManualDecisionAndUpdatedEvidenceWithoutChangingState(Decision decision) {
        AuthorizationRequest request = givenRequest();
        request.addEvidence(new ClinicalJustification("Updated evidence", null, null), clock);
        Review review = Review.createNewManualReview(request.getId(), clock);
        UUID reviewerId = UUID.randomUUID();
        review.updateStatusToManuallyDecided(decision, "Clinical review completed", reviewerId, decision == Decision.APPROVED ? 3 : null, clock);
        if (decision == Decision.APPROVED) {
            request.updateStatusToManuallyApproved();
        } else {
            request.updateStatusToManuallyRejected();
        }
        when(reviewRepository.findByRequestId(request.getId())).thenReturn(Optional.of(review));

        var details = requestQueryService.getProviderRequestDetails(request.getId(), providerId);

        assertEquals(request.getRequestStatus(), details.request().requestStatus());
        assertEquals(request.getRequestStatusReason(), details.request().requestStatusReason());
        assertEquals(clock.instant(), details.request().evidenceUpdatedAt());
        assertEquals(submittedAt, details.request().submittedAt());
        assertEquals("Original evidence\n\nUpdated evidence", details.clinicalJustification().summary());
        assertEquals(new BigDecimal("35.1"), details.clinicalJustification().observations().getFirst().value());
        assertEquals(review.getId(), details.request().reviewId());
        assertEquals(ReviewStatus.DECIDED, details.request().reviewStatus());
        assertEquals(decision, details.review().decision());
        assertEquals(DecisionActor.REVIEWER, details.review().decidedBy());
        assertEquals(reviewerId, details.review().reviewerId());
        assertEquals("Clinical review completed", details.review().decisionReason());
        assertEquals(clock.instant(), details.review().decisionDate());
        assertEquals(decision == Decision.APPROVED ? 3 : null, details.review().approvedQuantity());
        assertEquals(decision == Decision.APPROVED ? clock.instant() : null, details.review().validFrom());
        assertEquals(decision == Decision.APPROVED ? clock.instant().plus(30, ChronoUnit.DAYS) : null, details.review().validTo());
        assertEquals(ReviewStatus.DECIDED, review.getReviewStatus());
    }

    AuthorizationRequest givenRequest() {
        AuthorizationRequest request = request();
        when(authorizationRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        return request;
    }

    AuthorizationRequest request() {
        return new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), providerId, UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", "PROCEDURE", "Test service", Date.from(submittedAt), 5),
                new ClinicalJustification("Original evidence", List.of(), List.of(
                        new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("35.1"), "%", null, submittedAt)))
        );
    }
}

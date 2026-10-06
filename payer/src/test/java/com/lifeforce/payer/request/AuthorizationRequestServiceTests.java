package com.lifeforce.payer.request;

import com.lifeforce.payer.plan.domain.plan.BenefitStatus;
import com.lifeforce.payer.plan.domain.plan.Plan;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.plan.repository.PlanRepository;
import com.lifeforce.payer.reference.domain.NetworkParticipation;
import com.lifeforce.payer.reference.repository.NetworkParticipationRepository;
import com.lifeforce.payer.reference.repository.OrganizationRepository;
import com.lifeforce.payer.reference.repository.PatientRepository;
import com.lifeforce.payer.reference.repository.ProviderRepository;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.Coverage;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.repository.CoverageRepository;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class AuthorizationRequestServiceTests {
    @Mock AuthorizationRequestRepository authorizationRequestRepository;
    @Mock CoverageRepository coverageRepository;
    @Mock PlanRepository planRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;
    @Mock NetworkParticipationRepository networkParticipationRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock PatientRepository patientRepository;
    @Mock ProviderRepository providerRepository;

    AuthorizationRequestService authorizationRequestService;
    UUID payerId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        authorizationRequestService = new AuthorizationRequestService(
                authorizationRequestRepository, coverageRepository, planRepository, reviewRepository,
                reviewHistoryRepository, networkParticipationRepository, organizationRepository,
                patientRepository, providerRepository, clock
        );
    }

    @Test
    void savesValidRequestAsSubmitted() {
        HttpAuthorizationRequest request = request("PA");
        givenValidReferences();

        var response = authorizationRequestService.createAuthorizationRequest(request);

        ArgumentCaptor<AuthorizationRequest> savedRequest = ArgumentCaptor.forClass(AuthorizationRequest.class);
        verify(authorizationRequestRepository).save(savedRequest.capture());
        assertEquals(request.requestId(), response.requestId());
        assertEquals(ResponseStatus.SUBMITTED, response.responseStatus());
        assertEquals(request.requestId(), savedRequest.getValue().getId());
        assertEquals(RequestStatus.SUBMITTED, savedRequest.getValue().getRequestStatus());
        assertEquals(request.requestedService(), savedRequest.getValue().getRequestedService());
        assertEquals(request.clinicalJustification(), savedRequest.getValue().getClinicalJustification());
        verifyNoInteractions(reviewRepository, reviewHistoryRepository);
    }

    @Test
    void rejectsDuplicateBeforeCheckingReferencesOrSaving() {
        HttpAuthorizationRequest request = request("PA");
        when(authorizationRequestRepository.existsById(request.requestId())).thenReturn(true);

        var response = authorizationRequestService.createAuthorizationRequest(request);

        assertEquals(ResponseStatus.DUPLICATE_REJECTED, response.responseStatus());
        verify(authorizationRequestRepository, never()).save(any());
        verifyNoInteractions(patientRepository, organizationRepository, providerRepository, planRepository);
    }

    @Test
    void rejectsUnknownPatient() {
        assertMalformedRequest("Patient does not exist");
        verifyNoInteractions(organizationRepository, providerRepository, planRepository);
    }

    @Test
    void rejectsUnknownOrganization() {
        when(patientRepository.existsById(patientId)).thenReturn(true);
        assertMalformedRequest("Organization does not exist");
        verifyNoInteractions(providerRepository, planRepository);
    }

    @Test
    void rejectsProviderOutsideOrganization() {
        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        assertMalformedRequest("Provider does not exist or does not belong to organization");
        verify(providerRepository).existsByIdAndOrganizationId(providerId, organizationId);
        verifyNoInteractions(planRepository);
    }

    @Test
    void rejectsUnknownPlan() {
        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(providerRepository.existsByIdAndOrganizationId(providerId, organizationId)).thenReturn(true);
        assertMalformedRequest("Plan does not exist");
    }

    @Test
    void ignoresMissingRequest() {
        authorizationRequestService.processSubmittedRequest(UUID.randomUUID());

        verify(authorizationRequestRepository, never()).save(any());
        verifyNoInteractions(coverageRepository, planRepository, reviewRepository, reviewHistoryRepository);
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "SUBMITTED")
    void ignoresRequestsThatAreAlreadyProcessed(RequestStatus status) {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        switch (status) {
            case PENDING -> request.updateStatusToManualReview();
            case APPROVED -> request.updateStatusToAutoApproved();
            case REJECTED -> request.updateStatusToNotCovered();
            default -> throw new IllegalArgumentException("Expected a processed request status");
        }

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertEquals(status, request.getRequestStatus());
        verify(authorizationRequestRepository, never()).save(any());
        verifyNoInteractions(coverageRepository, planRepository, reviewRepository, reviewHistoryRepository);
    }

    @Test
    void rejectsRequestWithoutCoverage() {
        AuthorizationRequest request = givenSubmittedRequest("PA");

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.REJECTED, RequestStatusReason.NOT_COVERED);
        verifyNoInteractions(planRepository, networkParticipationRepository);
        assertNoReview();
    }

    @ParameterizedTest
    @CsvSource({
            "2019-06-02T00:00:00Z,2020-01-01T00:00:00Z",
            "2019-01-01T00:00:00Z,2019-06-01T00:00:00Z"
    })
    void rejectsCoverageBeforeStartOrAtEnd(Instant start, Instant end) {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenCoverage(List.of(coverage(start, end)));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.REJECTED, RequestStatusReason.COVERAGE_INACTIVE);
        verifyNoInteractions(planRepository, networkParticipationRepository);
        assertNoReview();
    }

    @Test
    void acceptsCoverageAtStartWithNoEnd() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenCoverage(List.of(coverage(submittedAt, null)));
        givenPlanWithService(BenefitStatus.COVERED, false);
        givenNetwork(List.of(network(true, submittedAt, null)));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.APPROVED, RequestStatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertNoReview();
    }

    @Test
    void selectsActiveCoverageFromMultiplePeriods() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenCoverage(List.of(
                coverage(submittedAt.minusSeconds(100), submittedAt),
                coverage(submittedAt, submittedAt.plusSeconds(100))
        ));
        givenPlanWithService(BenefitStatus.COVERED, false);
        givenNetwork(List.of(network(true, submittedAt, null)));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.APPROVED, RequestStatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertNoReview();
    }

    @Test
    void createsManualReviewWhenPlanIsMissing() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenActiveCoverage();

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.PENDING, RequestStatusReason.MANUAL_REVIEW_REQUIRED);
        assertCreatedReview(request, ReviewStatus.PENDING_MANUAL_REVIEW);
        verifyNoInteractions(networkParticipationRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "false,2019-01-01T00:00:00Z,2020-01-01T00:00:00Z",
            "true,2019-06-02T00:00:00Z,2020-01-01T00:00:00Z",
            "true,2019-01-01T00:00:00Z,2019-06-01T00:00:00Z"
    })
    void rejectsInactiveNetworkBeforeCheckingUnknownService(boolean inNetwork, Instant start, Instant end) {
        AuthorizationRequest request = givenSubmittedRequest("UNKNOWN");
        givenActiveCoverage();
        givenPlan(List.of());
        givenNetwork(List.of(network(inNetwork, start, end)));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.REJECTED, RequestStatusReason.OUT_OF_NETWORK);
        assertNoReview();
    }

    @Test
    void rejectsMissingNetworkParticipation() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenActiveCoverage();
        givenPlanWithService(BenefitStatus.COVERED, false);

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.REJECTED, RequestStatusReason.OUT_OF_NETWORK);
        assertNoReview();
    }

    @Test
    void selectsActiveNetworkFromMultiplePeriods() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenActiveCoverage();
        givenPlanWithService(BenefitStatus.COVERED, false);
        givenNetwork(List.of(
                network(false, submittedAt.minusSeconds(100), submittedAt),
                network(true, submittedAt, submittedAt.plusSeconds(100)),
                network(false, submittedAt.plusSeconds(100), null)
        ));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.APPROVED, RequestStatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertNoReview();
    }

    @Test
    void createsManualReviewForUnknownService() {
        AuthorizationRequest request = givenSubmittedRequest("UNKNOWN");
        givenActiveCoverage();
        givenPlan(List.of());
        givenNetwork(List.of(network(true, submittedAt, null)));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.PENDING, RequestStatusReason.MANUAL_REVIEW_REQUIRED);
        assertCreatedReview(request, ReviewStatus.PENDING_MANUAL_REVIEW);
    }

    @Test
    void rejectsExcludedServiceWithoutCreatingReview() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenActiveCoverage();
        givenPlanWithService(BenefitStatus.EXCLUDED, false);
        givenNetwork(List.of(network(true, submittedAt, null)));

        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.REJECTED, RequestStatusReason.SERVICE_EXCLUDED);
        assertNoReview();
    }

    @Test
    void createsEvaluationReviewAndHistoryOnlyOnce() {
        AuthorizationRequest request = givenSubmittedRequest("PA");
        givenActiveCoverage();
        givenPlanWithService(BenefitStatus.COVERED, true);
        givenNetwork(List.of(network(true, submittedAt, null)));

        authorizationRequestService.processSubmittedRequest(request.getId());
        authorizationRequestService.processSubmittedRequest(request.getId());

        assertSavedStatus(request, RequestStatus.PENDING, RequestStatusReason.PENDING_EVALUATION);
        assertCreatedReview(request, ReviewStatus.PENDING_EVALUATION);
    }

    void givenValidReferences() {
        when(patientRepository.existsById(patientId)).thenReturn(true);
        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(providerRepository.existsByIdAndOrganizationId(providerId, organizationId)).thenReturn(true);
        when(planRepository.existsById(planId)).thenReturn(true);
    }

    void assertMalformedRequest(String message) {
        var response = authorizationRequestService.createAuthorizationRequest(request("PA"));
        assertEquals(ResponseStatus.MALFORMED_REQUEST, response.responseStatus());
        assertEquals(message, response.message());
        verify(authorizationRequestRepository, never()).save(any());
        assertNoReview();
    }

    HttpAuthorizationRequest request(String code) {
        return new HttpAuthorizationRequest(
                UUID.randomUUID(), submittedAt, patientId, providerId, organizationId, planId,
                new RequestedService(code, null, null, Date.from(submittedAt), 5),
                new ClinicalJustification(null, List.of(), List.of())
        );
    }

    AuthorizationRequest givenSubmittedRequest(String code) {
        AuthorizationRequest request = new AuthorizationRequest().build(request(code));
        when(authorizationRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        return request;
    }

    void givenActiveCoverage() {
        givenCoverage(List.of(coverage(submittedAt, null)));
    }

    void givenCoverage(List<Coverage> coverages) {
        when(coverageRepository.findByPatientIdAndPlanId(patientId, planId)).thenReturn(coverages);
    }

    Coverage coverage(Instant start, Instant end) {
        Coverage coverage = new Coverage();
        ReflectionTestUtils.setField(coverage, "start", start);
        ReflectionTestUtils.setField(coverage, "end", end);
        return coverage;
    }

    void givenPlanWithService(BenefitStatus benefitStatus, boolean priorAuthorizationRequired) {
        PlanService planService = new PlanService();
        ReflectionTestUtils.setField(planService, "code", "PA");
        ReflectionTestUtils.setField(planService, "benefitStatus", benefitStatus);
        ReflectionTestUtils.setField(planService, "priorAuthorizationRequired", priorAuthorizationRequired);
        givenPlan(List.of(planService));
    }

    void givenPlan(List<PlanService> services) {
        Plan plan = new Plan();
        ReflectionTestUtils.setField(plan, "payerId", payerId);
        ReflectionTestUtils.setField(plan, "planServices", services);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
    }

    void givenNetwork(List<NetworkParticipation> networkParticipations) {
        when(networkParticipationRepository.findByPayerIdAndOrganizationId(payerId, organizationId)).thenReturn(networkParticipations);
    }

    NetworkParticipation network(boolean inNetwork, Instant start, Instant end) {
        NetworkParticipation network = new NetworkParticipation();
        ReflectionTestUtils.setField(network, "inNetwork", inNetwork);
        ReflectionTestUtils.setField(network, "effectiveFrom", start);
        ReflectionTestUtils.setField(network, "effectiveTo", end);
        return network;
    }

    void assertSavedStatus(AuthorizationRequest request, RequestStatus status, RequestStatusReason reason) {
        assertEquals(status, request.getRequestStatus());
        assertEquals(reason, request.getRequestStatusReason());
        verify(authorizationRequestRepository).save(request);
    }

    void assertNoReview() {
        verifyNoInteractions(reviewRepository, reviewHistoryRepository);
    }

    void assertCreatedReview(AuthorizationRequest request, ReviewStatus status) {
        ArgumentCaptor<Review> savedReview = ArgumentCaptor.forClass(Review.class);
        ArgumentCaptor<ReviewHistory> savedHistory = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewRepository).save(savedReview.capture());
        verify(reviewHistoryRepository).save(savedHistory.capture());
        Review review = savedReview.getValue();
        ReviewHistory history = savedHistory.getValue();
        assertEquals(request.getId(), review.getRequestId());
        assertEquals(status, review.getReviewStatus());
        assertEquals(clock.instant(), review.getLastUpdated());
        assertEquals(review.getId(), history.getReviewId());
        assertEquals("REVIEW_CREATED", history.getEventType());
        assertEquals("SYSTEM", history.getEventSource());
        assertEquals(clock.instant(), history.getEventAt());
        assertEquals(status, history.getEventPayload().get("reviewStatus"));
        assertEquals(RequestStatus.PENDING, history.getEventPayload().get("requestStatus"));
        assertEquals(request.getRequestStatusReason(), history.getEventPayload().get("statusReason"));
        assertNull(history.getEventPayload().get("decision"));
    }
}

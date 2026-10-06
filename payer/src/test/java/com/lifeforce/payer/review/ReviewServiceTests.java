package com.lifeforce.payer.review;

import com.lifeforce.payer.plan.domain.plan.CodeType;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.repository.PlanServiceRepository;
import com.lifeforce.payer.plan.service.PlanEvalService;
import com.lifeforce.payer.plan.service.PolicyEvaluationResult;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.RequestedEvidence;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.service.ReviewService;
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
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTests {
    @Mock PlanEvalService planEvalService;
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;
    @Mock PlanServiceRepository planServiceRepository;
    @Mock AuthorizationRequestRepository authorizationRequestRepository;

    ReviewService reviewService;
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        reviewService = new ReviewService(planEvalService, reviewRepository, reviewHistoryRepository, planServiceRepository, authorizationRequestRepository, clock);
    }

    @Test
    void ignoresMissingReview() {
        reviewService.evaluateReview(UUID.randomUUID());

        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(planServiceRepository, planEvalService, reviewHistoryRepository, authorizationRequestRepository);
    }

    @ParameterizedTest
    @EnumSource(value = ReviewStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING_EVALUATION")
    void ignoresReviewsThatAreNotPendingEvaluation(ReviewStatus status) {
        Review review = givenPendingReview();
        ReflectionTestUtils.setField(review, "reviewStatus", status);

        reviewService.evaluateReview(review.getId());

        assertEquals(status, review.getReviewStatus());
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(planServiceRepository, planEvalService, reviewHistoryRepository, authorizationRequestRepository);
    }

    @Test
    void approvesMatchedPolicyAndPreservesQuantityAndDecisionDates() {
        Review review = givenPendingReview();
        givenPolicyResult(review, new Policy(), PolicyEvaluationResult.MATCHED);

        reviewService.evaluateReview(review.getId());

        assertOutcome(review, ReviewStatus.DECIDED, RequestStatus.APPROVED, RequestStatusReason.AUTO_APPROVED);
        assertEquals(Decision.APPROVED, review.getDecision());
        assertEquals(DecisionActor.SYSTEM, review.getDecidedBy());
        assertEquals(5, review.getApprovedQuantity());
        assertEquals(clock.instant(), review.getDecisionDate());
        assertEquals(clock.instant(), review.getValidFrom());
        assertEquals(clock.instant().plus(30, ChronoUnit.DAYS), review.getValidTo());
        assertSavedHistory(review);
    }

    @ParameterizedTest
    @CsvSource({
            "AWAITING_EVIDENCE,AWAITING_EVIDENCE,AWAITING_EVIDENCE",
            "CRITERIA_NOT_MET,PENDING_MANUAL_REVIEW,CRITERIA_NOT_MET",
            "MANUAL_REVIEW_REQUIRED,PENDING_MANUAL_REVIEW,MANUAL_REVIEW_REQUIRED"
    })
    void routesEvaluationResultWithoutMakingDecision(PolicyEvaluationResult result, ReviewStatus status, RequestStatusReason reason) {
        Review review = givenPendingReview();
        givenPolicyResult(review, new Policy(), result);

        reviewService.evaluateReview(review.getId());

        assertOutcome(review, status, RequestStatus.PENDING, reason);
        assertNull(review.getDecision());
        assertNull(review.getDecisionDate());
        assertNull(review.getApprovedQuantity());
        assertNull(review.getValidFrom());
        assertNull(review.getValidTo());
        assertSavedHistory(review);
    }

    @Test
    void routesMissingServiceToManualReviewWithoutEvaluatingPolicy() {
        Review review = givenPendingReview();

        reviewService.evaluateReview(review.getId());

        assertOutcome(review, ReviewStatus.PENDING_MANUAL_REVIEW, RequestStatus.PENDING, RequestStatusReason.MANUAL_REVIEW_REQUIRED);
        verify(planServiceRepository).findByPlanIdAndCodeAndCodeType(review.getAuthorizationRequest().getPlanId(), "PA", CodeType.PROCEDURE);
        verifyNoInteractions(planEvalService);
        assertSavedHistory(review);
    }

    @Test
    void delegatesMissingPolicyToEvaluator() {
        Review review = givenPendingReview();
        givenPolicyResult(review, null, PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED);

        reviewService.evaluateReview(review.getId());

        assertOutcome(review, ReviewStatus.PENDING_MANUAL_REVIEW, RequestStatus.PENDING, RequestStatusReason.MANUAL_REVIEW_REQUIRED);
        assertSavedHistory(review);
    }

    @Test
    void doesNotEvaluateOrWriteHistoryAgainAfterApproval() {
        Review review = givenPendingReview();
        Policy policy = new Policy();
        givenPolicyResult(review, policy, PolicyEvaluationResult.MATCHED);

        reviewService.evaluateReview(review.getId());
        reviewService.evaluateReview(review.getId());

        verify(planEvalService).evalPolicyForEvidence(policy, review.getAuthorizationRequest().getClinicalJustification(), submittedAt);
        assertOutcome(review, ReviewStatus.DECIDED, RequestStatus.APPROVED, RequestStatusReason.AUTO_APPROVED);
        assertSavedHistory(review);
    }

    @Test
    void evaluatesUpdatedEvidenceAtItsSubmissionTimeInsteadOfOriginalRequestTime() {
        Review review = givenPendingReview();
        AuthorizationRequest request = review.getAuthorizationRequest();
        request.addEvidence(new ClinicalJustification("New results", null, null), clock);
        review.updateStatusToPendingEvaluation(clock);
        Policy policy = new Policy();
        PlanService planService = new PlanService();
        ReflectionTestUtils.setField(planService, "policy", policy);
        when(planServiceRepository.findByPlanIdAndCodeAndCodeType(request.getPlanId(), "PA", CodeType.PROCEDURE)).thenReturn(Optional.of(planService));
        when(planEvalService.evalPolicyForEvidence(policy, request.getClinicalJustification(), clock.instant())).thenReturn(PolicyEvaluationResult.MATCHED);

        reviewService.evaluateReview(review.getId());

        verify(planEvalService).evalPolicyForEvidence(policy, request.getClinicalJustification(), clock.instant());
        assertEquals(submittedAt, request.getSubmittedAt());
        assertOutcome(review, ReviewStatus.DECIDED, RequestStatus.APPROVED, RequestStatusReason.AUTO_APPROVED);
        assertSavedHistory(review);
    }

    @Test
    void doesNotWriteHistoryWhenReviewSaveFails() {
        Review review = givenPendingReview();
        givenPolicyResult(review, new Policy(), PolicyEvaluationResult.MATCHED);
        IllegalStateException failure = new IllegalStateException("Review unavailable");
        doThrow(failure).when(reviewRepository).save(review);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> reviewService.evaluateReview(review.getId())));
        verifyNoInteractions(reviewHistoryRepository);
    }

    @Test
    void savesStructuredMissingEvidenceWithTheReviewAndHistory() {
        Review review = givenPendingReview();
        Policy policy = new Policy();
        givenPolicyResult(review, policy, PolicyEvaluationResult.AWAITING_EVIDENCE);

        reviewService.evaluateReview(review.getId());

        assertEquals(List.of("CHF"), review.getEvidenceRequest().requestedConditions());
        assertEquals(List.of("EF"), review.getEvidenceRequest().requestedObservations());
        verify(planEvalService).getRequestedEvidence(policy, review.getAuthorizationRequest().getClinicalJustification(), submittedAt);
        assertOutcome(review, ReviewStatus.AWAITING_EVIDENCE, RequestStatus.PENDING, RequestStatusReason.AWAITING_EVIDENCE);
        assertSavedHistory(review);
    }

    @Test
    void doesNotSaveReviewOrHistoryWhenRequestSaveFails() {
        Review review = givenPendingReview();
        givenPolicyResult(review, new Policy(), PolicyEvaluationResult.MATCHED);
        IllegalStateException failure = new IllegalStateException("Request unavailable");
        doThrow(failure).when(authorizationRequestRepository).save(review.getAuthorizationRequest());

        assertSame(failure, assertThrows(IllegalStateException.class, () -> reviewService.evaluateReview(review.getId())));
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(reviewHistoryRepository);
    }

    Review givenPendingReview() {
        AuthorizationRequest request = new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", null, null, Date.from(submittedAt), 5),
                new ClinicalJustification(null, List.of(), List.of())
        );
        request.updateStatusToPendingEvaluation();
        Review review = Review.createNewReview(request.getId(), Clock.fixed(submittedAt, ZoneOffset.UTC));
        ReflectionTestUtils.setField(review, "authorizationRequest", request);
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        return review;
    }

    void givenPolicyResult(Review review, Policy policy, PolicyEvaluationResult result) {
        AuthorizationRequest request = review.getAuthorizationRequest();
        PlanService planService = new PlanService();
        ReflectionTestUtils.setField(planService, "policy", policy);
        when(planServiceRepository.findByPlanIdAndCodeAndCodeType(request.getPlanId(), "PA", CodeType.PROCEDURE)).thenReturn(Optional.of(planService));
        when(planEvalService.evalPolicyForEvidence(policy, request.getClinicalJustification(), submittedAt)).thenReturn(result);
        if (result == PolicyEvaluationResult.AWAITING_EVIDENCE) {
            when(planEvalService.getRequestedEvidence(policy, request.getClinicalJustification(), submittedAt))
                    .thenReturn(new RequestedEvidence("Provide missing evidence", List.of("CHF"), List.of("EF"), null));
        }
    }

    void assertOutcome(Review review, ReviewStatus status, RequestStatus requestStatus, RequestStatusReason reason) {
        assertEquals(status, review.getReviewStatus());
        assertEquals(requestStatus, review.getAuthorizationRequest().getRequestStatus());
        assertEquals(reason, review.getAuthorizationRequest().getRequestStatusReason());
        assertEquals(clock.instant(), review.getLastUpdated());
        verify(authorizationRequestRepository).save(review.getAuthorizationRequest());
        verify(reviewRepository).save(review);
    }

    void assertSavedHistory(Review review) {
        ArgumentCaptor<ReviewHistory> savedHistory = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewHistoryRepository).save(savedHistory.capture());
        ReviewHistory history = savedHistory.getValue();
        assertEquals(review.getId(), history.getReviewId());
        assertEquals("SYSTEM", history.getEventSource());
        assertEquals(review.getAuthorizationRequest().getRequestStatusReason().name(), history.getEventType());
        assertEquals(clock.instant(), history.getEventAt());
        assertEquals(review.getReviewStatus(), history.getEventPayload().get("reviewStatus"));
        assertEquals(review.getAuthorizationRequest().getRequestStatus(), history.getEventPayload().get("requestStatus"));
        assertEquals(review.getAuthorizationRequest().getRequestStatusReason(), history.getEventPayload().get("statusReason"));
        assertEquals(review.getDecision(), history.getEventPayload().get("decision"));
        assertEquals(review.getApprovedQuantity(), history.getEventPayload().get("approvedQuantity"));
        assertEquals(review.getEvidenceRequest(), history.getEventPayload().get("evidenceRequest"));
    }
}

package com.lifeforce.payer.review;

import com.lifeforce.payer.plan.domain.plan.CodeType;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import com.lifeforce.payer.plan.domain.policy.EvidenceType;
import com.lifeforce.payer.plan.domain.policy.Match;
import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.domain.policy.PolicyCriterion;
import com.lifeforce.payer.plan.domain.policy.PolicyCriterionOperator;
import com.lifeforce.payer.plan.domain.policy.ReviewMode;
import com.lifeforce.payer.plan.repository.PlanServiceRepository;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.domain.RequestedService;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class ReviewQueryServiceTests {
    @Mock ReviewRepository reviewRepository;
    @Mock PlanServiceRepository planServiceRepository;
    @Mock ReviewHistoryRepository reviewHistoryRepository;

    ReviewQueryService reviewQueryService;
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Clock clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void createService() {
        reviewQueryService = new ReviewQueryService(reviewRepository, planServiceRepository, reviewHistoryRepository);
    }

    @AfterEach
    void verifiesQueriesDoNotSaveReviews() {
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void returnsEmptyQueueWhenNoManualReviewsExist() {
        assertTrue(reviewQueryService.getReviews(ReviewStatus.PENDING_MANUAL_REVIEW).isEmpty());
        verify(reviewRepository).findByReviewStatusOrderByLastUpdatedAscIdAsc(ReviewStatus.PENDING_MANUAL_REVIEW);
        verifyNoInteractions(planServiceRepository);
    }

    @Test
    void returnsQueueSummariesInRepositoryOrder() {
        Review firstReview = review();
        Review secondReview = review();
        when(reviewRepository.findByReviewStatusOrderByLastUpdatedAscIdAsc(ReviewStatus.PENDING_MANUAL_REVIEW))
                .thenReturn(List.of(firstReview, secondReview));

        var summaries = reviewQueryService.getReviews(ReviewStatus.PENDING_MANUAL_REVIEW);

        assertEquals(List.of(firstReview.getId(), secondReview.getId()), summaries.stream().map(summary -> summary.id()).toList());
        var firstSummary = summaries.getFirst();
        AuthorizationRequest request = firstReview.getAuthorizationRequest();
        assertEquals(request.getId(), firstSummary.requestId());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, firstSummary.reviewStatus());
        assertEquals(RequestStatus.PENDING, firstSummary.requestStatus());
        assertEquals(RequestStatusReason.MANUAL_REVIEW_REQUIRED, firstSummary.requestStatusReason());
        assertEquals(request.getPatientId(), firstSummary.patientId());
        assertEquals(request.getProviderId(), firstSummary.providerId());
        assertEquals(request.getOrganizationId(), firstSummary.organizationId());
        assertEquals(request.getPlanId(), firstSummary.planId());
        assertEquals(request.getRequestedService(), firstSummary.requestedService().toDomain());
        assertEquals(submittedAt, firstSummary.submittedAt());
        assertEquals(clock.instant(), firstSummary.lastUpdated());
        verifyNoInteractions(planServiceRepository);
    }

    @Test
    void passesRequestedQueueStatusToRepository() {
        reviewQueryService.getReviews(ReviewStatus.AWAITING_EVIDENCE);

        verify(reviewRepository).findByReviewStatusOrderByLastUpdatedAscIdAsc(ReviewStatus.AWAITING_EVIDENCE);
        verifyNoInteractions(planServiceRepository);
    }

    @Test
    void returnsEmptyDetailsWhenReviewDoesNotExist() {
        UUID reviewId = UUID.randomUUID();

        assertTrue(reviewQueryService.getReviewDetails(reviewId).isEmpty());
        verify(reviewRepository).findById(reviewId);
        verifyNoInteractions(planServiceRepository);
    }

    @Test
    void returnsRequestEvidenceAndPolicyCriteria() {
        Review review = givenReview();
        Policy policy = policy();
        givenPlanService(review, policy);

        ReviewDetails details = reviewQueryService.getReviewDetails(review.getId()).orElseThrow();

        AuthorizationRequest request = review.getAuthorizationRequest();
        assertEquals(review.getId(), details.id());
        assertEquals(review.getReviewStatus(), details.reviewStatus());
        assertEquals(clock.instant(), details.lastUpdated());
        assertEquals(request.getId(), details.request().id());
        assertEquals(request.getPatientId(), details.request().patientId());
        assertEquals(request.getProviderId(), details.request().providerId());
        assertEquals(request.getOrganizationId(), details.request().organizationId());
        assertEquals(request.getPlanId(), details.request().planId());
        assertEquals(request.getRequestStatus(), details.request().requestStatus());
        assertEquals(request.getRequestStatusReason(), details.request().requestStatusReason());
        assertEquals(request.getRequestedService(), details.request().requestedService().toDomain());
        assertEquals(request.getClinicalJustification(), details.request().clinicalJustification().toDomain());
        assertEquals(submittedAt, details.request().submittedAt());
        assertNull(details.decision());
        assertEquals(policy.getId(), details.policy().id());
        assertEquals(ReviewMode.AUTO_APPROVAL_ELIGIBLE, details.policy().reviewMode());
        assertEquals(Match.ALL, details.policy().match());
        var criterion = details.policy().criteria().getFirst();
        assertEquals(policy.getPolicyCriteria().getFirst().getId(), criterion.id());
        assertEquals(EvidenceType.OBSERVATION, criterion.evidenceType());
        assertEquals("EF", criterion.code());
        assertEquals(PolicyCriterionOperator.LTE, criterion.operator());
        assertEquals(new BigDecimal("35"), criterion.value());
        assertEquals("%", criterion.unit());
        assertEquals(RequestStatus.PENDING, request.getRequestStatus());
        assertEquals(ReviewStatus.PENDING_MANUAL_REVIEW, review.getReviewStatus());
    }

    @Test
    void returnsDetailsWithoutPolicyWhenServiceIsUnconfigured() {
        Review review = givenReview();

        ReviewDetails details = reviewQueryService.getReviewDetails(review.getId()).orElseThrow();

        assertEquals(review.getId(), details.id());
        assertNotNull(details.request());
        assertNull(details.policy());
        verify(planServiceRepository).findByPlanIdAndCodeAndCodeType(review.getAuthorizationRequest().getPlanId(), "PA", CodeType.PROCEDURE);
    }

    @Test
    void returnsDetailsWithoutPolicyWhenPolicyIsUnconfigured() {
        Review review = givenReview();
        givenPlanService(review, null);

        assertNull(reviewQueryService.getReviewDetails(review.getId()).orElseThrow().policy());
    }

    @Test
    void returnsPolicyWithEmptyCriteria() {
        Review review = givenReview();
        Policy policy = policy();
        ReflectionTestUtils.setField(policy, "policyCriteria", List.of());
        givenPlanService(review, policy);

        assertTrue(reviewQueryService.getReviewDetails(review.getId()).orElseThrow().policy().criteria().isEmpty());
    }

    @Test
    void returnsDecisionFieldsForCompletedReview() {
        Review review = givenReview();
        review.updateStatusToAutoApproved(5, clock);
        review.getAuthorizationRequest().updateStatusToAutoApproved();
        UUID reviewerId = UUID.randomUUID();
        ReflectionTestUtils.setField(review, "reviewerId", reviewerId);

        ReviewDetails details = reviewQueryService.getReviewDetails(review.getId()).orElseThrow();

        assertEquals(ReviewStatus.DECIDED, details.reviewStatus());
        assertEquals(RequestStatus.APPROVED, details.request().requestStatus());
        assertEquals(Decision.APPROVED, details.decision());
        assertEquals("Auto Approved", details.decisionReason());
        assertEquals(DecisionActor.SYSTEM, details.decidedBy());
        assertEquals(reviewerId, details.reviewerId());
        assertEquals(5, details.approvedQuantity());
        assertEquals(clock.instant(), details.decisionDate());
        assertEquals(review.getValidFrom(), details.validFrom());
        assertEquals(review.getValidTo(), details.validTo());
    }

    Review givenReview() {
        Review review = review();
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
        return review;
    }

    Review review() {
        ClinicalJustification evidence = new ClinicalJustification("Clinical summary", List.of(), List.of(
                new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("35.1"), "%", null, submittedAt)
        ));
        AuthorizationRequest request = new AuthorizationRequest().build(
                UUID.randomUUID(), submittedAt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new RequestedService("PA", "PROCEDURE", "Test service", Date.from(submittedAt), 5), evidence
        );
        request.updateStatusToManualReview();
        Review review = Review.createNewManualReview(request.getId(), clock);
        ReflectionTestUtils.setField(review, "authorizationRequest", request);
        return review;
    }

    void givenPlanService(Review review, Policy policy) {
        PlanService planService = new PlanService();
        ReflectionTestUtils.setField(planService, "policy", policy);
        when(planServiceRepository.findByPlanIdAndCodeAndCodeType(review.getAuthorizationRequest().getPlanId(), "PA", CodeType.PROCEDURE))
                .thenReturn(Optional.of(planService));
    }

    Policy policy() {
        PolicyCriterion criterion = new PolicyCriterion();
        ReflectionTestUtils.setField(criterion, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(criterion, "evidenceType", EvidenceType.OBSERVATION);
        ReflectionTestUtils.setField(criterion, "code", "EF");
        ReflectionTestUtils.setField(criterion, "operator", PolicyCriterionOperator.LTE);
        ReflectionTestUtils.setField(criterion, "value", new BigDecimal("35"));
        ReflectionTestUtils.setField(criterion, "unit", "%");
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(policy, "reviewMode", ReviewMode.AUTO_APPROVAL_ELIGIBLE);
        ReflectionTestUtils.setField(policy, "match", Match.ALL);
        ReflectionTestUtils.setField(policy, "policyCriteria", List.of(criterion));
        return policy;
    }
}

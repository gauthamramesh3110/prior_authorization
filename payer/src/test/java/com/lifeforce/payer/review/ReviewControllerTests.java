package com.lifeforce.payer.review;

import com.lifeforce.payer.plan.domain.policy.EvidenceType;
import com.lifeforce.payer.plan.domain.policy.Match;
import com.lifeforce.payer.plan.domain.policy.PolicyCriterionOperator;
import com.lifeforce.payer.plan.domain.policy.ReviewMode;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.review.controller.ReviewController;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.dto.ReviewSummary;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTests {
    @Mock ReviewQueryService reviewQueryService;
    MockMvc mockMvc;
    UUID reviewId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");

    @BeforeEach
    void createController() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewQueryService)).build();
    }

    @Test
    void returnsManualQueueByDefault() throws Exception {
        ReviewSummary summary = new ReviewSummary(
                reviewId, requestId, ReviewStatus.PENDING_MANUAL_REVIEW, RequestStatus.PENDING,
                RequestStatusReason.CRITERIA_NOT_MET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                requestedService(), submittedAt, submittedAt
        );
        when(reviewQueryService.getReviews(ReviewStatus.PENDING_MANUAL_REVIEW)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId.toString()))
                .andExpect(jsonPath("$[0].requestId").value(requestId.toString()))
                .andExpect(jsonPath("$[0].reviewStatus").value("PENDING_MANUAL_REVIEW"))
                .andExpect(jsonPath("$[0].requestStatusReason").value("CRITERIA_NOT_MET"))
                .andExpect(jsonPath("$[0].requestedService.code").value("PA"))
                .andExpect(jsonPath("$[0].clinicalJustification").doesNotExist());
        verify(reviewQueryService).getReviews(ReviewStatus.PENDING_MANUAL_REVIEW);
    }

    @Test
    void returnsEmptyArrayForEmptyQueue() throws Exception {
        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(reviewQueryService).getReviews(ReviewStatus.PENDING_MANUAL_REVIEW);
    }

    @ParameterizedTest
    @EnumSource(ReviewStatus.class)
    void passesStatusFilterToService(ReviewStatus status) throws Exception {
        mockMvc.perform(get("/api/v1/reviews").param("status", status.name()))
                .andExpect(status().isOk());
        verify(reviewQueryService).getReviews(status);
    }

    @Test
    void rejectsInvalidStatusBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/reviews").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewQueryService);
    }

    @Test
    void returnsReviewDetailsWithEvidenceAndPolicy() throws Exception {
        ReviewDetails.PolicyDetails policy = new ReviewDetails.PolicyDetails(
                UUID.randomUUID(), ReviewMode.AUTO_APPROVAL_ELIGIBLE, Match.ALL,
                List.of(new ReviewDetails.CriterionDetails(UUID.randomUUID(), EvidenceType.OBSERVATION, "EF", PolicyCriterionOperator.LTE, new BigDecimal("35"), "%"))
        );
        when(reviewQueryService.getReviewDetails(reviewId)).thenReturn(Optional.of(details(policy)));

        mockMvc.perform(get("/api/v1/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value("PENDING_MANUAL_REVIEW"))
                .andExpect(jsonPath("$.request.id").value(requestId.toString()))
                .andExpect(jsonPath("$.request.submittedAt").value(submittedAt.toString()))
                .andExpect(jsonPath("$.request.clinicalJustification.observations[0].value").value(35.1))
                .andExpect(jsonPath("$.policy.id").value(policy.id().toString()))
                .andExpect(jsonPath("$.policy.criteria[0].operator").value("LTE"))
                .andExpect(jsonPath("$.policy.criteria[0].value").value(35))
                .andExpect(jsonPath("$.policy.criteria[0].policy").doesNotExist());
        verify(reviewQueryService).getReviewDetails(reviewId);
    }

    @Test
    void returnsDetailsWhenPolicyIsMissing() throws Exception {
        when(reviewQueryService.getReviewDetails(reviewId)).thenReturn(Optional.of(details(null)));

        mockMvc.perform(get("/api/v1/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.id").value(requestId.toString()))
                .andExpect(jsonPath("$.policy").value(nullValue()));
    }

    @Test
    void returnsNotFoundForUnknownReview() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/{id}", reviewId))
                .andExpect(status().isNotFound());
        verify(reviewQueryService).getReviewDetails(reviewId);
    }

    @Test
    void rejectsInvalidReviewIdBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewQueryService);
    }

    ReviewDetails details(ReviewDetails.PolicyDetails policy) {
        ClinicalJustification evidence = new ClinicalJustification("Clinical summary", List.of(), List.of(
                new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("35.1"), "%", null, submittedAt)
        ));
        ReviewDetails.RequestDetails request = new ReviewDetails.RequestDetails(
                requestId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                RequestStatus.PENDING, RequestStatusReason.CRITERIA_NOT_MET, requestedService(), evidence, submittedAt
        );
        return new ReviewDetails(
                reviewId, ReviewStatus.PENDING_MANUAL_REVIEW, submittedAt, null, null, null, null, null,
                null, null, null, request, policy
        );
    }

    RequestedService requestedService() {
        return new RequestedService("PA", "PROCEDURE", "Test service", Date.from(submittedAt), 5);
    }
}

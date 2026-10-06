package com.lifeforce.payer.review;

import com.lifeforce.payer.common.ApiExceptionHandler;
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
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.dto.ManualDecisionRequest;
import com.lifeforce.payer.review.dto.ManualDecisionResponse;
import com.lifeforce.payer.review.service.ReviewDecisionService;
import com.lifeforce.payer.review.service.ReviewEvidenceService;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.dto.ReviewSummary;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTests {
    @Mock ReviewQueryService reviewQueryService;
    @Mock ReviewDecisionService reviewDecisionService;
    @Mock ReviewEvidenceService reviewEvidenceService;
    LocalValidatorFactoryBean validator;
    MockMvc mockMvc;
    UUID reviewerId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");

    @BeforeEach
    void createController() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewQueryService, reviewDecisionService, reviewEvidenceService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator).build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Review does not exist"));
        verify(reviewQueryService).getReviewDetails(reviewId);
    }

    @Test
    void rejectsInvalidReviewIdBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewQueryService);
    }

    @Test
    void submitsManualApprovalAndReturnsDecision() throws Exception {
        when(reviewDecisionService.submitManualDecision(eq(reviewId), any())).thenReturn(decisionResponse(Decision.APPROVED));

        mockMvc.perform(post("/api/v1/reviews/{id}/decision", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(decisionBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value("DECIDED"))
                .andExpect(jsonPath("$.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.requestStatusReason").value("MANUAL_APPROVED"))
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.decisionReason").value("Clinical review completed"))
                .andExpect(jsonPath("$.decidedBy").value("REVIEWER"))
                .andExpect(jsonPath("$.reviewerId").value(reviewerId.toString()))
                .andExpect(jsonPath("$.approvedQuantity").value(3))
                .andExpect(jsonPath("$.decisionDate").value(submittedAt.toString()))
                .andExpect(jsonPath("$.validFrom").value(submittedAt.toString()))
                .andExpect(jsonPath("$.validTo").value(submittedAt.plus(30, ChronoUnit.DAYS).toString()));
        ArgumentCaptor<ManualDecisionRequest> request = ArgumentCaptor.forClass(ManualDecisionRequest.class);
        verify(reviewDecisionService).submitManualDecision(eq(reviewId), request.capture());
        assertEquals(reviewerId, request.getValue().reviewerId());
        assertEquals(Decision.APPROVED, request.getValue().decision());
        assertEquals("Clinical review completed", request.getValue().decisionReason());
        assertEquals(3, request.getValue().approvedQuantity());
        verifyNoInteractions(reviewQueryService);
    }

    @Test
    void submitsManualRejectionWithoutApprovedQuantity() throws Exception {
        when(reviewDecisionService.submitManualDecision(eq(reviewId), any())).thenReturn(decisionResponse(Decision.REJECTED));
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(decisionBody());
        body.put("decision", "REJECTED");
        body.remove("approvedQuantity");

        mockMvc.perform(post("/api/v1/reviews/{id}/decision", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("REJECTED"))
                .andExpect(jsonPath("$.requestStatusReason").value("MANUAL_REJECTED"))
                .andExpect(jsonPath("$.decision").value("REJECTED"))
                .andExpect(jsonPath("$.approvedQuantity").value(nullValue()))
                .andExpect(jsonPath("$.validFrom").value(nullValue()))
                .andExpect(jsonPath("$.validTo").value(nullValue()));
        ArgumentCaptor<ManualDecisionRequest> request = ArgumentCaptor.forClass(ManualDecisionRequest.class);
        verify(reviewDecisionService).submitManualDecision(eq(reviewId), request.capture());
        assertEquals(Decision.REJECTED, request.getValue().decision());
        assertEquals(null, request.getValue().approvedQuantity());
        verifyNoInteractions(reviewQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"reviewerId", "decision", "decisionReason"})
    void rejectsMissingDecisionFieldsBeforeCallingService(String field) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(decisionBody());
        body.putNull(field);

        assertInvalidDecisionBody(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankDecisionReasonBeforeCallingService(String reason) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(decisionBody());
        body.put("decisionReason", reason);

        assertInvalidDecisionBody(body.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveQuantityBeforeCallingService(int quantity) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(decisionBody());
        body.put("approvedQuantity", quantity);

        assertInvalidDecisionBody(body.toString());
    }

    @Test
    void rejectsUnknownDecisionBeforeCallingService() throws Exception {
        assertInvalidDecisionBody(decisionBody().replace("APPROVED", "UNKNOWN"));
    }

    @Test
    void rejectsInvalidReviewerIdBeforeCallingService() throws Exception {
        assertInvalidDecisionBody(decisionBody().replace(reviewerId.toString(), "not-a-uuid"));
    }

    @Test
    void rejectsMalformedDecisionJsonBeforeCallingService() throws Exception {
        assertInvalidDecisionBody("{");
    }

    @Test
    void rejectsInvalidReviewIdForDecisionBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/not-a-uuid/decision")
                        .contentType(MediaType.APPLICATION_JSON).content(decisionBody()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewDecisionService, reviewQueryService);
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "NOT_FOUND", "CONFLICT"})
    void returnsServiceFailureStatusForManualDecision(HttpStatus failureStatus) throws Exception {
        when(reviewDecisionService.submitManualDecision(eq(reviewId), any()))
                .thenThrow(new ResponseStatusException(failureStatus, "Decision could not be submitted"));

        mockMvc.perform(post("/api/v1/reviews/{id}/decision", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(decisionBody()))
                .andExpect(status().is(failureStatus.value()))
                .andExpect(jsonPath("$.status").value(failureStatus.value()))
                .andExpect(jsonPath("$.detail").value("Decision could not be submitted"));
        verify(reviewDecisionService).submitManualDecision(eq(reviewId), any());
        verifyNoInteractions(reviewQueryService);
    }

    @Test
    void explainsMissingDecisionReason() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/{id}/decision", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(decisionBody().replace("Clinical review completed", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("decisionReason"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
        verifyNoInteractions(reviewDecisionService, reviewQueryService);
    }

    void assertInvalidDecisionBody(String body) throws Exception {
        mockMvc.perform(post("/api/v1/reviews/{id}/decision", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewDecisionService, reviewQueryService);
    }

    String decisionBody() {
        return """
                {"reviewerId":"%s","decision":"APPROVED","decisionReason":"Clinical review completed","approvedQuantity":3}
                """.formatted(reviewerId);
    }

    ManualDecisionResponse decisionResponse(Decision decision) {
        boolean approved = decision == Decision.APPROVED;
        return new ManualDecisionResponse(
                reviewId, requestId, ReviewStatus.DECIDED,
                approved ? RequestStatus.APPROVED : RequestStatus.REJECTED,
                approved ? RequestStatusReason.MANUAL_APPROVED : RequestStatusReason.MANUAL_REJECTED,
                decision, "Clinical review completed", submittedAt, DecisionActor.REVIEWER, reviewerId,
                approved ? 3 : null, approved ? submittedAt : null,
                approved ? submittedAt.plus(30, ChronoUnit.DAYS) : null
        );
    }

    @Test
    void returnsEvidenceRequestAndEvidenceUpdateTimeInReviewDetails() throws Exception {
        ReviewDetails original = details(null);
        ReviewDetails.RequestDetails request = original.request();
        var evidenceRequest = new com.lifeforce.payer.review.dto.RequestedEvidence("Provide clinical evidence", List.of("CHF"), List.of("EF"), "Report");
        ReviewDetails response = new ReviewDetails(
                original.id(), ReviewStatus.PENDING_MANUAL_REVIEW, original.lastUpdated(), null, null, null, null, null,
                null, null, null, new ReviewDetails.RequestDetails(
                request.id(), request.patientId(), request.providerId(), request.organizationId(), request.planId(),
                request.requestStatus(), request.requestStatusReason(), request.requestedService(), request.clinicalJustification(),
                request.submittedAt(), submittedAt.plusSeconds(60)), null, evidenceRequest
        );
        when(reviewQueryService.getReviewDetails(reviewId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/reviews/{id}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceRequest.summary").value("Provide clinical evidence"))
                .andExpect(jsonPath("$.evidenceRequest.requestedConditions[0]").value("CHF"))
                .andExpect(jsonPath("$.evidenceRequest.requestedObservations[0]").value("EF"))
                .andExpect(jsonPath("$.evidenceRequest.otherEvidence").value("Report"))
                .andExpect(jsonPath("$.request.evidenceUpdatedAt").value(submittedAt.plusSeconds(60).toString()));
    }

    ReviewDetails details(ReviewDetails.PolicyDetails policy) {
        ClinicalJustification evidence = new ClinicalJustification("Clinical summary", List.of(), List.of(
                new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("35.1"), "%", null, submittedAt)
        ));
        ReviewDetails.RequestDetails request = new ReviewDetails.RequestDetails(
                requestId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                RequestStatus.PENDING, RequestStatusReason.CRITERIA_NOT_MET, requestedService(), evidence, submittedAt, null
        );
        return new ReviewDetails(
                reviewId, ReviewStatus.PENDING_MANUAL_REVIEW, submittedAt, null, null, null, null, null,
                null, null, null, request, policy, null
        );
    }

    RequestedService requestedService() {
        return new RequestedService("PA", "PROCEDURE", "Test service", Date.from(submittedAt), 5);
    }
}

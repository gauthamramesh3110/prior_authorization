package com.lifeforce.payer.review;

import com.lifeforce.payer.common.ApiExceptionHandler;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.review.controller.ReviewController;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.EvidenceRequest;
import com.lifeforce.payer.review.dto.RequestedEvidence;
import com.lifeforce.payer.review.dto.EvidenceRequestResponse;
import com.lifeforce.payer.review.service.ReviewDecisionService;
import com.lifeforce.payer.review.service.ReviewEvidenceService;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewEvidenceControllerTests {
    @Mock ReviewQueryService reviewQueryService;
    @Mock ReviewDecisionService reviewDecisionService;
    @Mock ReviewEvidenceService reviewEvidenceService;

    MockMvc mockMvc;
    LocalValidatorFactoryBean validator;
    UUID reviewId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    Instant requestedAt = Instant.parse("2026-10-05T12:00:00Z");

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
        verifyNoInteractions(reviewQueryService, reviewDecisionService);
    }

    @Test
    void requestsEvidenceAndReturnsUpdatedStatusesAndRequestedItems() throws Exception {
        EvidenceRequestResponse response = new EvidenceRequestResponse(
                reviewId, requestId, reviewerId, new RequestedEvidence("Please provide supporting evidence", List.of("CHF"), List.of("EF"), "Echocardiogram report"), requestedAt,
                ReviewStatus.AWAITING_EVIDENCE, RequestStatus.PENDING, RequestStatusReason.AWAITING_EVIDENCE
        );
        when(reviewEvidenceService.requestEvidence(eq(reviewId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/{id}/evidence-requests", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.reviewerId").value(reviewerId.toString()))
                .andExpect(jsonPath("$.evidenceRequest.summary").value(response.evidenceRequest().summary()))
                .andExpect(jsonPath("$.evidenceRequest.requestedConditions[0]").value("CHF"))
                .andExpect(jsonPath("$.evidenceRequest.requestedObservations[0]").value("EF"))
                .andExpect(jsonPath("$.evidenceRequest.otherEvidence").value("Echocardiogram report"))
                .andExpect(jsonPath("$.requestedAt").value(requestedAt.toString()))
                .andExpect(jsonPath("$.reviewStatus").value("AWAITING_EVIDENCE"))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"))
                .andExpect(jsonPath("$.requestStatusReason").value("AWAITING_EVIDENCE"));
        ArgumentCaptor<EvidenceRequest> request = ArgumentCaptor.forClass(EvidenceRequest.class);
        verify(reviewEvidenceService).requestEvidence(eq(reviewId), request.capture());
        assertEquals(reviewerId, request.getValue().reviewerId());
        assertEquals(response.evidenceRequest(), request.getValue().evidenceRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"reviewerId", "evidenceRequest"})
    void rejectsMissingFieldsBeforeCallingService(String field) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        body.putNull(field);

        assertInvalidBody(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankSummaryBeforeCallingService(String summary) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        body.withObject("evidenceRequest").put("summary", summary);

        assertInvalidBody(body.toString());
    }

    @Test
    void rejectsMissingSummaryBeforeCallingService() throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        body.withObject("evidenceRequest").putNull("summary");

        assertInvalidBody(body.toString());
    }

    @Test
    void acceptsAnEvidenceRequestWithOnlySummaryAndOtherEvidence() throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        body.withObject("evidenceRequest").remove("requestedConditions");
        body.withObject("evidenceRequest").remove("requestedObservations");

        mockMvc.perform(post("/api/v1/reviews/{id}/evidence-requests", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk());
        verify(reviewEvidenceService).requestEvidence(eq(reviewId), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsInvalidRequestedItemBeforeCallingService(String item) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        body.withObject("evidenceRequest").putArray("requestedObservations").add(item);

        assertInvalidBody(body.toString());
    }

    @Test
    void rejectsInvalidReviewerIdBeforeCallingService() throws Exception {
        assertInvalidBody(requestBody().replace(reviewerId.toString(), "not-a-uuid"));
    }

    @Test
    void rejectsInvalidReviewIdBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/not-a-uuid/evidence-requests")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewEvidenceService);
    }

    @Test
    void rejectsMalformedJsonBeforeCallingService() throws Exception {
        assertInvalidBody("{");
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "NOT_FOUND", "CONFLICT"})
    void returnsServiceFailureStatus(HttpStatus failureStatus) throws Exception {
        when(reviewEvidenceService.requestEvidence(eq(reviewId), any()))
                .thenThrow(new ResponseStatusException(failureStatus, "Evidence could not be requested"));

        mockMvc.perform(post("/api/v1/reviews/{id}/evidence-requests", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().is(failureStatus.value()))
                .andExpect(jsonPath("$.status").value(failureStatus.value()))
                .andExpect(jsonPath("$.detail").value("Evidence could not be requested"));
        verify(reviewEvidenceService).requestEvidence(eq(reviewId), any());
    }

    @Test
    void explainsMissingEvidenceRequestSummary() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/{id}/evidence-requests", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody().replace("Please provide supporting evidence", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("evidenceRequest.summary"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
        verifyNoInteractions(reviewEvidenceService);
    }

    void assertInvalidBody(String body) throws Exception {
        mockMvc.perform(post("/api/v1/reviews/{id}/evidence-requests", reviewId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewEvidenceService);
    }

    String requestBody() {
        return """
                {"reviewerId":"%s","evidenceRequest":{"summary":"Please provide supporting evidence","requestedConditions":["CHF"],"requestedObservations":["EF"],"otherEvidence":"Echocardiogram report"}}
                """.formatted(reviewerId);
    }
}

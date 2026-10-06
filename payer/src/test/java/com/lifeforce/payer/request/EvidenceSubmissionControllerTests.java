package com.lifeforce.payer.request;

import com.lifeforce.payer.request.controller.AuthorizationRequestController;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.EvidenceSubmission;
import com.lifeforce.payer.request.dto.EvidenceSubmissionResponse;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.request.service.EvidenceSubmissionService;
import com.lifeforce.payer.request.service.RequestQueryService;
import com.lifeforce.payer.review.domain.ReviewStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EvidenceSubmissionControllerTests {
    @Mock AuthorizationRequestService authorizationRequestService;
    @Mock EvidenceSubmissionService evidenceSubmissionService;
    @Mock RequestQueryService requestQueryService;

    MockMvc mockMvc;
    LocalValidatorFactoryBean validator;
    UUID requestId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();
    Instant evidenceUpdatedAt = Instant.parse("2026-10-05T12:00:00Z");

    @BeforeEach
    void createController() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthorizationRequestController(authorizationRequestService, evidenceSubmissionService, requestQueryService))
                .setValidator(validator).build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
        verifyNoInteractions(authorizationRequestService);
    }

    @ParameterizedTest
    @EnumSource(value = ReviewStatus.class, names = {"PENDING_EVALUATION", "PENDING_MANUAL_REVIEW"})
    void submitsEvidenceAndReturnsQueuedReviewStatus(ReviewStatus reviewStatus) throws Exception {
        EvidenceSubmissionResponse response = new EvidenceSubmissionResponse(
                requestId, UUID.randomUUID(), providerId, reviewStatus,
                RequestStatus.PENDING, RequestStatusReason.EVIDENCE_UPDATED, evidenceUpdatedAt
        );
        when(evidenceSubmissionService.submitEvidence(eq(requestId), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/requests/{id}/evidence", requestId)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.reviewId").value(response.reviewId().toString()))
                .andExpect(jsonPath("$.providerId").value(providerId.toString()))
                .andExpect(jsonPath("$.reviewStatus").value(reviewStatus.name()))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"))
                .andExpect(jsonPath("$.requestStatusReason").value("EVIDENCE_UPDATED"))
                .andExpect(jsonPath("$.evidenceUpdatedAt").value(evidenceUpdatedAt.toString()));
        ArgumentCaptor<EvidenceSubmission> submission = ArgumentCaptor.forClass(EvidenceSubmission.class);
        verify(evidenceSubmissionService).submitEvidence(eq(requestId), submission.capture());
        assertEquals(providerId, submission.getValue().providerId());
        assertEquals("New test results", submission.getValue().clinicalJustification().summary());
        assertEquals("HF", submission.getValue().clinicalJustification().conditions().getFirst().code());
        assertEquals(new BigDecimal("35.1"), submission.getValue().clinicalJustification().observations().getFirst().value());
        assertEquals(evidenceUpdatedAt, submission.getValue().clinicalJustification().observations().getFirst().recordedAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"providerId", "clinicalJustification"})
    void rejectsMissingRequiredFieldsBeforeCallingService(String field) throws Exception {
        ObjectNode body = body();
        body.putNull(field);

        assertInvalidBody(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "startDate"})
    void rejectsInvalidConditionBeforeCallingService(String field) throws Exception {
        ObjectNode body = body();
        ((ObjectNode) body.get("clinicalJustification").get("conditions").get(0)).putNull(field);

        assertInvalidBody(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "value", "units", "recordedAt"})
    void rejectsInvalidObservationBeforeCallingService(String field) throws Exception {
        ObjectNode body = body();
        ((ObjectNode) body.get("clinicalJustification").get("observations").get(0)).putNull(field);

        assertInvalidBody(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"conditions", "observations"})
    void rejectsNullEvidenceItemsBeforeCallingService(String field) throws Exception {
        ObjectNode body = body();
        ((ObjectNode) body.get("clinicalJustification")).putArray(field).addNull();

        assertInvalidBody(body.toString());
    }

    @Test
    void rejectsInvalidProviderIdBeforeCallingService() throws Exception {
        assertInvalidBody(requestBody().replace(providerId.toString(), "not-a-uuid"));
    }

    @Test
    void rejectsInvalidRequestIdBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/api/v1/requests/not-a-uuid/evidence")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(evidenceSubmissionService);
    }

    @Test
    void rejectsMalformedJsonBeforeCallingService() throws Exception {
        assertInvalidBody("{");
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "FORBIDDEN", "NOT_FOUND", "CONFLICT"})
    void returnsServiceFailureStatus(HttpStatus failureStatus) throws Exception {
        when(evidenceSubmissionService.submitEvidence(eq(requestId), any()))
                .thenThrow(new ResponseStatusException(failureStatus, "Evidence could not be submitted"));

        mockMvc.perform(patch("/api/v1/requests/{id}/evidence", requestId)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().is(failureStatus.value()));
        verify(evidenceSubmissionService).submitEvidence(eq(requestId), any());
    }

    ObjectNode body() {
        return (ObjectNode) new ObjectMapper().readTree(requestBody());
    }

    void assertInvalidBody(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/requests/{id}/evidence", requestId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(evidenceSubmissionService);
    }

    String requestBody() {
        return """
                {"providerId":"%s","clinicalJustification":{
                  "summary":"New test results",
                  "conditions":[{"code":"HF","startDate":"2026-10-05T10:00:00Z"}],
                  "observations":[{"code":"EF","value":35.1,"units":"%%","recordedAt":"2026-10-05T12:00:00Z"}]
                }}
                """.formatted(providerId);
    }
}

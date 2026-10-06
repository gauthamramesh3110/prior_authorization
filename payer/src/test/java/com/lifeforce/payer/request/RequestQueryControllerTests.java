package com.lifeforce.payer.request;

import com.lifeforce.payer.common.ApiExceptionHandler;
import com.lifeforce.payer.request.controller.AuthorizationRequestController;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.RequestStatusReason;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.RequestDetails;
import com.lifeforce.payer.request.dto.RequestSummary;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.request.service.EvidenceSubmissionService;
import com.lifeforce.payer.request.service.RequestQueryService;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.DecisionActor;
import com.lifeforce.payer.review.domain.ReviewStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RequestQueryControllerTests {
    @Mock AuthorizationRequestService authorizationRequestService;
    @Mock EvidenceSubmissionService evidenceSubmissionService;
    @Mock RequestQueryService requestQueryService;

    MockMvc mockMvc;
    UUID requestId = UUID.randomUUID();
    UUID providerId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");
    Instant updatedAt = Instant.parse("2026-10-06T12:00:00Z");

    @BeforeEach
    void createController() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthorizationRequestController(authorizationRequestService, evidenceSubmissionService, requestQueryService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @AfterEach
    void verifiesTrackingDoesNotInvokeWorkflowServices() {
        verifyNoInteractions(authorizationRequestService, evidenceSubmissionService);
    }

    @Test
    void returnsProviderRequestsWithStatusReasonsAndReviewIdentifiers() throws Exception {
        when(requestQueryService.getProviderRequests(providerId, null)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/requests").param("providerId", providerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId.toString()))
                .andExpect(jsonPath("$[0].providerId").value(providerId.toString()))
                .andExpect(jsonPath("$[0].requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].requestStatusReason").value("MANUAL_APPROVED"))
                .andExpect(jsonPath("$[0].requestedService.code").value("PA"))
                .andExpect(jsonPath("$[0].submittedAt").value(submittedAt.toString()))
                .andExpect(jsonPath("$[0].evidenceUpdatedAt").value(updatedAt.toString()))
                .andExpect(jsonPath("$[0].reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$[0].reviewStatus").value("DECIDED"))
                .andExpect(jsonPath("$[0].clinicalJustification").doesNotExist());
        verify(requestQueryService).getProviderRequests(providerId, null);
    }

    @Test
    void returnsEmptyArrayForProviderWithoutRequests() throws Exception {
        mockMvc.perform(get("/api/v1/requests").param("providerId", providerId.toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @EnumSource(RequestStatus.class)
    void passesProviderAndStatusFilterToService(RequestStatus requestStatus) throws Exception {
        mockMvc.perform(get("/api/v1/requests").param("providerId", providerId.toString()).param("status", requestStatus.name()))
                .andExpect(status().isOk());
        verify(requestQueryService).getProviderRequests(providerId, requestStatus);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/requests", "/api/v1/requests/03842da4-07b7-4ffa-93ba-cb0da47d2d20"})
    void rejectsMissingProviderIdBeforeCallingService(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint)).andExpect(status().isBadRequest());
        verifyNoInteractions(requestQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/requests", "/api/v1/requests/03842da4-07b7-4ffa-93ba-cb0da47d2d20"})
    void rejectsInvalidProviderIdBeforeCallingService(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint).param("providerId", "not-a-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(requestQueryService);
    }

    @Test
    void rejectsInvalidStatusBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/requests").param("providerId", providerId.toString()).param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestQueryService);
    }

    @Test
    void returnsNotFoundForUnknownProvider() throws Exception {
        when(requestQueryService.getProviderRequests(providerId, null)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider does not exist"));

        mockMvc.perform(get("/api/v1/requests").param("providerId", providerId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Provider does not exist"));
    }

    @Test
    void returnsDetailsWithCurrentEvidenceAndDecision() throws Exception {
        RequestDetails.ReviewDetails review = new RequestDetails.ReviewDetails(
                reviewId, ReviewStatus.DECIDED, updatedAt, Decision.APPROVED, "Approved following clinical review", updatedAt,
                DecisionActor.REVIEWER, UUID.randomUUID(), 3, updatedAt, updatedAt.plus(30, ChronoUnit.DAYS),
                new com.lifeforce.payer.review.dto.RequestedEvidence("Provide clinical evidence", List.of("CHF"), List.of("EF"), "Report")
        );
        ClinicalJustification evidence = new ClinicalJustification("Updated evidence", List.of(), List.of(
                new ClinicalJustification.ObservationEvidence("EF", new BigDecimal("35.1"), "%", null, updatedAt)
        ));
        when(requestQueryService.getProviderRequestDetails(requestId, providerId)).thenReturn(new RequestDetails(summary(), evidence, review));

        mockMvc.perform(get("/api/v1/requests/{id}", requestId).param("providerId", providerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.id").value(requestId.toString()))
                .andExpect(jsonPath("$.request.providerId").value(providerId.toString()))
                .andExpect(jsonPath("$.request.requestStatusReason").value("MANUAL_APPROVED"))
                .andExpect(jsonPath("$.request.requestedService.code").value("PA"))
                .andExpect(jsonPath("$.request.submittedAt").value(submittedAt.toString()))
                .andExpect(jsonPath("$.review.evidenceRequest.summary").value("Provide clinical evidence"))
                .andExpect(jsonPath("$.review.evidenceRequest.requestedConditions[0]").value("CHF"))
                .andExpect(jsonPath("$.review.evidenceRequest.requestedObservations[0]").value("EF"))
                .andExpect(jsonPath("$.review.evidenceRequest.otherEvidence").value("Report"))
                .andExpect(jsonPath("$.clinicalJustification.summary").value("Updated evidence"))
                .andExpect(jsonPath("$.clinicalJustification.observations[0].value").value(35.1))
                .andExpect(jsonPath("$.review.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.review.reviewStatus").value("DECIDED"))
                .andExpect(jsonPath("$.review.decision").value("APPROVED"))
                .andExpect(jsonPath("$.review.decisionReason").value(review.decisionReason()))
                .andExpect(jsonPath("$.review.decidedBy").value("REVIEWER"))
                .andExpect(jsonPath("$.review.reviewerId").value(review.reviewerId().toString()))
                .andExpect(jsonPath("$.review.approvedQuantity").value(3))
                .andExpect(jsonPath("$.review.validTo").value(review.validTo().toString()));
        verify(requestQueryService).getProviderRequestDetails(requestId, providerId);
    }

    @Test
    void returnsSubmittedRequestWithoutReview() throws Exception {
        RequestSummary submitted = new RequestSummary(requestId, UUID.randomUUID(), providerId, UUID.randomUUID(), UUID.randomUUID(),
                RequestStatus.SUBMITTED, null, summary().requestedService(), submittedAt, null, null, null);
        when(requestQueryService.getProviderRequestDetails(requestId, providerId)).thenReturn(new RequestDetails(submitted, null, null));

        mockMvc.perform(get("/api/v1/requests/{id}", requestId).param("providerId", providerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.requestStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.request.reviewId").value(nullValue()))
                .andExpect(jsonPath("$.review").value(nullValue()));
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"NOT_FOUND", "FORBIDDEN"})
    void returnsRequestDetailsFailureStatus(HttpStatus failureStatus) throws Exception {
        when(requestQueryService.getProviderRequestDetails(requestId, providerId)).thenThrow(new ResponseStatusException(failureStatus, "Request is unavailable"));

        mockMvc.perform(get("/api/v1/requests/{id}", requestId).param("providerId", providerId.toString()))
                .andExpect(status().is(failureStatus.value()))
                .andExpect(jsonPath("$.status").value(failureStatus.value()))
                .andExpect(jsonPath("$.detail").value("Request is unavailable"));
    }

    @Test
    void rejectsInvalidRequestIdBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/requests/not-a-uuid").param("providerId", providerId.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(requestQueryService);
    }

    RequestSummary summary() {
        return new RequestSummary(requestId, UUID.randomUUID(), providerId, UUID.randomUUID(), UUID.randomUUID(),
                RequestStatus.APPROVED, RequestStatusReason.MANUAL_APPROVED,
                new RequestedService("PA", "PROCEDURE", "Test service", Date.from(submittedAt), 5),
                submittedAt, updatedAt, reviewId, ReviewStatus.DECIDED);
    }
}

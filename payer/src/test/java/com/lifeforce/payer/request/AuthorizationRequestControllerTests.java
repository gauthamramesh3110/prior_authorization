package com.lifeforce.payer.request;

import com.lifeforce.payer.request.controller.AuthorizationRequestController;
import com.lifeforce.payer.request.dto.AuthorizationSubmission;
import com.lifeforce.payer.request.dto.AuthorizationSubmissionResponse;
import com.lifeforce.payer.request.dto.SubmissionStatus;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.request.service.EvidenceSubmissionService;
import com.lifeforce.payer.request.service.RequestQueryService;
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
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestControllerTests {
    @Mock AuthorizationRequestService authorizationRequestService;
    @Mock EvidenceSubmissionService evidenceSubmissionService;
    @Mock RequestQueryService requestQueryService;
    MockMvc mockMvc;
    LocalValidatorFactoryBean validator;
    UUID requestId = UUID.randomUUID();

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
    }

    @Test
    void returnsCreatedForSubmittedRequest() throws Exception {
        when(authorizationRequestService.submitAuthorizationRequest(any())).thenReturn(
                new AuthorizationSubmissionResponse(requestId, SubmissionStatus.SUBMITTED, "Request has been submitted")
        );

        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.responseStatus").value("SUBMITTED"));

        ArgumentCaptor<AuthorizationSubmission> request = ArgumentCaptor.forClass(AuthorizationSubmission.class);
        verify(authorizationRequestService).submitAuthorizationRequest(request.capture());
        assertEquals(requestId, request.getValue().requestId());
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "CONFLICT"})
    void returnsStatusFromServiceException(HttpStatus failureStatus) throws Exception {
        when(authorizationRequestService.submitAuthorizationRequest(any()))
                .thenThrow(new ResponseStatusException(failureStatus, "Rejected"));

        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().is(failureStatus.value()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"requestId", "submittedAt", "patientId", "providerId", "organizationId", "planId", "requestedService", "clinicalJustification"})
    void rejectsMissingRequiredFieldsBeforeCallingService(String field) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        body.putNull(field);

        assertMalformedRequest(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null"})
    void rejectsInvalidQuantityBeforeCallingService(String quantity) throws Exception {
        assertMalformedRequest(requestBody().replace("\"quantity\":5", "\"quantity\":" + quantity));
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "startDate"})
    void rejectsInvalidConditionBeforeCallingService(String field) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        ObjectNode condition = body.withObject("clinicalJustification").putArray("conditions").addObject();
        condition.put("code", "CONDITION");
        condition.put("startDate", "2019-01-01");
        if (field.equals("code")) {
            condition.put("code", "");
        } else {
            condition.putNull(field);
        }

        assertMalformedRequest(body.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "value", "units", "recordedAt"})
    void rejectsInvalidObservationBeforeCallingService(String field) throws Exception {
        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(requestBody());
        ObjectNode observation = body.withObject("clinicalJustification").putArray("observations").addObject();
        observation.put("code", "EF");
        observation.put("value", new BigDecimal("35.1"));
        observation.put("units", "%");
        observation.put("recordedAt", "2019-06-01T00:00:00Z");
        if (field.equals("code")) {
            observation.put("code", "");
        } else {
            observation.putNull(field);
        }

        assertMalformedRequest(body.toString());
    }

    @Test
    void rejectsMalformedJsonBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authorizationRequestService);
    }

    @Test
    void passesDecimalObservationToServiceWithoutRounding() throws Exception {
        when(authorizationRequestService.submitAuthorizationRequest(any())).thenReturn(
                new AuthorizationSubmissionResponse(requestId, SubmissionStatus.SUBMITTED, "Submitted")
        );
        String body = requestBody().replace("\"observations\":[]", "\"observations\":[{\"code\":\"EF\",\"value\":35.1,\"units\":\"%\",\"recordedAt\":\"2019-06-01T00:00:00Z\"}]");

        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<AuthorizationSubmission> request = ArgumentCaptor.forClass(AuthorizationSubmission.class);
        verify(authorizationRequestService).submitAuthorizationRequest(request.capture());
        assertEquals(new BigDecimal("35.1"), request.getValue().clinicalJustification().observations().getFirst().value());
    }

    void assertMalformedRequest(String body) throws Exception {
        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authorizationRequestService);
    }

    String requestBody() {
        return """
                {"requestId":"%s","submittedAt":"2019-06-01T00:00:00Z","patientId":"%s","providerId":"%s","organizationId":"%s","planId":"%s",
                "requestedService":{"code":"PA","requestedDate":"2019-06-01","quantity":5},
                "clinicalJustification":{"conditions":[],"observations":[]}}
                """.formatted(requestId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}

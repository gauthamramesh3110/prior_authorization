package com.lifeforce.payer.request;

import com.lifeforce.payer.request.domain.Status;
import com.lifeforce.payer.request.domain.StatusReason;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationRequestTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AuthorizationRequestService authorizationRequestService;

    @Autowired
    EntityManager entityManager;

    UUID payerId;
    UUID planId;
    UUID patientId;
    UUID organizationId;
    UUID providerId;

    @BeforeEach
    void createReferenceData() {
        payerId = UUID.randomUUID();
        planId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        providerId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO payer VALUES (?, 'Test payer')", payerId);
        jdbcTemplate.update("INSERT INTO plan VALUES (?, ?, 'Test plan')", planId, payerId);
        jdbcTemplate.update("INSERT INTO patient VALUES (?, 'Test patient')", patientId);
        jdbcTemplate.update("INSERT INTO organization VALUES (?, 'Test organization')", organizationId);
        jdbcTemplate.update("INSERT INTO provider VALUES (?, ?, 'Test provider')", providerId, organizationId);
        jdbcTemplate.update(
                "INSERT INTO coverage VALUES (?, ?, ?, '2019-01-01T00:00:00Z', '2020-01-01T00:00:00Z')",
                UUID.randomUUID(), patientId, planId
        );
        jdbcTemplate.update(
                "INSERT INTO network_participation VALUES (?, ?, true, '2019-01-01T00:00:00Z', null)",
                payerId, organizationId
        );
        jdbcTemplate.update(
                """
                INSERT INTO plan_service VALUES
                (?, ?, 'NO-PA', 'PROCEDURE', 'COVERED', false, null),
                (?, ?, 'PA', 'PROCEDURE', 'COVERED', true, null),
                (?, ?, 'EXCLUDED', 'PROCEDURE', 'EXCLUDED', false, null)
                """,
                UUID.randomUUID(), planId, UUID.randomUUID(), planId, UUID.randomUUID(), planId
        );
    }

    @Test
    void approvesCoveredRequestAtCoverageStart() throws Exception {
        UUID requestId = submitRequest("NO-PA", "2019-01-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.APPROVED, StatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertReviewCount(requestId, 0);
    }

    @Test
    void rejectsRequestAtCoverageEnd() throws Exception {
        UUID requestId = submitRequest("NO-PA", "2020-01-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.REJECTED, StatusReason.COVERAGE_INACTIVE);
        assertReviewCount(requestId, 0);
    }

    @Test
    void rejectsOutOfNetworkRequestBeforeCheckingService() throws Exception {
        jdbcTemplate.update("UPDATE network_participation SET in_network = false WHERE payer_id = ?", payerId);
        UUID requestId = submitRequest("UNKNOWN", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.REJECTED, StatusReason.OUT_OF_NETWORK);
        assertReviewCount(requestId, 0);
    }

    @Test
    void rejectsRequestWithoutNetworkParticipation() throws Exception {
        jdbcTemplate.update("DELETE FROM network_participation WHERE payer_id = ?", payerId);
        UUID requestId = submitRequest("NO-PA", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.REJECTED, StatusReason.OUT_OF_NETWORK);
    }

    @Test
    void checksNetworkDatesAtSubmissionTime() throws Exception {
        jdbcTemplate.update(
                "UPDATE network_participation SET effective_to = '2019-07-01T00:00:00Z' WHERE payer_id = ?",
                payerId
        );
        UUID activeRequestId = submitRequest("NO-PA", "2019-01-01T00:00:00Z");
        UUID expiredRequestId = submitRequest("NO-PA", "2019-07-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(activeRequestId);
        authorizationRequestService.processSubmittedRequest(expiredRequestId);

        assertRequestStatus(activeRequestId, Status.APPROVED, StatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertRequestStatus(expiredRequestId, Status.REJECTED, StatusReason.OUT_OF_NETWORK);
    }

    @Test
    void rejectsRequestBeforeNetworkParticipationStarts() throws Exception {
        jdbcTemplate.update(
                "UPDATE network_participation SET effective_from = '2019-07-01T00:00:00Z' WHERE payer_id = ?",
                payerId
        );
        UUID requestId = submitRequest("NO-PA", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.REJECTED, StatusReason.OUT_OF_NETWORK);
    }

    @Test
    void usesActiveNetworkParticipationWhenMultiplePeriodsExist() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO network_participation VALUES
                (?, ?, false, '2018-01-01T00:00:00Z', '2019-01-01T00:00:00Z'),
                (?, ?, false, '2020-01-01T00:00:00Z', null)
                """,
                payerId, organizationId, payerId, organizationId
        );
        UUID requestId = submitRequest("NO-PA", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.APPROVED, StatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertReviewCount(requestId, 0);
    }

    @Test
    void createsOneReviewWhenPendingRequestIsProcessedAgain() throws Exception {
        UUID requestId = submitRequest("PA", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);
        entityManager.flush();
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.PENDING, StatusReason.PENDING_EVALUATION);
        assertReviewCount(requestId, 1);
        assertEquals("PENDING_EVALUATION", jdbcTemplate.queryForObject(
                "SELECT status FROM review WHERE request_id = ?", String.class, requestId
        ));
    }

    @Test
    void preservesCompletedRequestWhenProcessedAgain() throws Exception {
        UUID requestId = submitRequest("NO-PA", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(requestId);
        entityManager.flush();
        jdbcTemplate.update("UPDATE network_participation SET in_network = false WHERE payer_id = ?", payerId);
        authorizationRequestService.processSubmittedRequest(requestId);

        assertRequestStatus(requestId, Status.APPROVED, StatusReason.PRIOR_AUTH_NOT_REQUIRED);
        assertReviewCount(requestId, 0);
    }

    @Test
    void preservesManualReviewAndExcludedServiceOutcomes() throws Exception {
        UUID manualRequestId = submitRequest("UNKNOWN", "2019-06-01T00:00:00Z");
        UUID excludedRequestId = submitRequest("EXCLUDED", "2019-06-01T00:00:00Z");
        authorizationRequestService.processSubmittedRequest(manualRequestId);
        authorizationRequestService.processSubmittedRequest(excludedRequestId);

        assertRequestStatus(manualRequestId, Status.PENDING, StatusReason.MANUAL_REVIEW_REQUIRED);
        assertReviewCount(manualRequestId, 1);
        assertEquals("PENDING_MANUAL_REVIEW", jdbcTemplate.queryForObject(
                "SELECT status FROM review WHERE request_id = ?", String.class, manualRequestId
        ));
        assertRequestStatus(excludedRequestId, Status.REJECTED, StatusReason.SERVICE_EXCLUDED);
        assertReviewCount(excludedRequestId, 0);
    }

    @Test
    void rejectsMalformedPayloadsBeforeSaving() throws Exception {
        UUID requestId = UUID.randomUUID();
        String body = requestBody(requestId, "NO-PA", "2019-06-01T00:00:00Z");
        List<String> invalidBodies = List.of(
                body.replace("\"organizationId\":\"" + organizationId + "\"", "\"organizationId\":null"),
                body.replace("{\"conditions\":[],\"observations\":[]}", "null"),
                body.replace("\"quantity\":1", "\"quantity\":0"),
                body.replace("\"quantity\":1", "\"quantity\":-1"),
                body.replace("\"conditions\":[]", "\"conditions\":[{\"code\":\"\",\"startDate\":null}]"),
                body.replace("\"observations\":[]", "\"observations\":[{\"code\":\"\",\"value\":null,\"units\":null,\"recordedAt\":null}]")
        );

        for (String invalidBody : invalidBodies) {
            mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                    .andExpect(status().isBadRequest());
            assertEquals(0, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM authorization_request WHERE id = ?", Integer.class, requestId
            ));
        }
    }

    @Test
    void rejectsUnknownReferencesAndProviderOrganizationMismatch() throws Exception {
        UUID otherOrganizationId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO organization VALUES (?, 'Other organization')", otherOrganizationId);
        UUID requestId = UUID.randomUUID();
        String body = requestBody(requestId, "NO-PA", "2019-06-01T00:00:00Z");
        List<String> invalidBodies = List.of(
                body.replace(patientId.toString(), UUID.randomUUID().toString()),
                body.replace(organizationId.toString(), UUID.randomUUID().toString()),
                body.replace(providerId.toString(), UUID.randomUUID().toString()),
                body.replace(planId.toString(), UUID.randomUUID().toString()),
                body.replace(organizationId.toString(), otherOrganizationId.toString())
        );

        for (String invalidBody : invalidBodies) {
            mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                    .andExpect(status().isBadRequest());
            assertEquals(0, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM authorization_request WHERE id = ?", Integer.class, requestId
            ));
        }
    }

    @Test
    void preservesDecimalObservationValues() throws Exception {
        UUID requestId = UUID.randomUUID();
        String body = requestBody(requestId, "PA", "2019-06-01T00:00:00Z").replace(
                "\"observations\":[]",
                "\"observations\":[{\"code\":\"EF\",\"value\":35.1,\"units\":\"%\",\"recordedAt\":\"2019-01-01T00:00:00Z\"}]"
        );
        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
        authorizationRequestService.processSubmittedRequest(requestId);

        BigDecimal storedValue = jdbcTemplate.queryForObject(
                "SELECT (clinical_justification->'observations'->0->>'value')::numeric FROM authorization_request WHERE id = ?",
                BigDecimal.class, requestId
        );
        assertEquals(0, new BigDecimal("35.1").compareTo(storedValue));
        assertRequestStatus(requestId, Status.PENDING, StatusReason.PENDING_EVALUATION);
    }

    UUID submitRequest(String code, String submittedAt) throws Exception {
        UUID requestId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(requestBody(requestId, code, submittedAt)))
                .andExpect(status().isCreated());
        return requestId;
    }

    String requestBody(UUID requestId, String code, String submittedAt) {
        return """
                {"requestId":"%s","submittedAt":"%s","patientId":"%s","providerId":"%s","organizationId":"%s","planId":"%s",
                "requestedService":{"code":"%s","requestedDate":"2019-06-01","quantity":1},
                "clinicalJustification":{"conditions":[],"observations":[]}}
                """.formatted(requestId, submittedAt, patientId, providerId, organizationId, planId, code);
    }

    void assertRequestStatus(UUID requestId, Status expectedStatus, StatusReason expectedReason) {
        entityManager.flush();
        assertEquals(expectedStatus.name(), jdbcTemplate.queryForObject(
                "SELECT status FROM authorization_request WHERE id = ?", String.class, requestId
        ));
        assertEquals(expectedReason.name(), jdbcTemplate.queryForObject(
                "SELECT status_reason FROM authorization_request WHERE id = ?", String.class, requestId
        ));
    }

    void assertReviewCount(UUID requestId, int expectedCount) {
        entityManager.flush();
        assertEquals(expectedCount, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review WHERE request_id = ?", Integer.class, requestId
        ));
    }
}

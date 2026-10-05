package com.lifeforce.payer.review;

import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.RequestedService;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Tag("integration")
class ReviewWorkflowIntegrationTests {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AuthorizationRequestService authorizationRequestService;
    @Autowired ReviewService reviewService;
    @MockitoSpyBean ReviewHistoryRepository reviewHistoryRepository;

    UUID payerId;
    UUID planId;
    UUID patientId;
    UUID organizationId;
    UUID providerId;
    UUID policyId;
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");

    @BeforeEach
    void createReferenceData() {
        payerId = UUID.randomUUID();
        planId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO payer VALUES (?, 'Test payer')", payerId);
        jdbcTemplate.update("INSERT INTO plan VALUES (?, ?, 'Test plan')", planId, payerId);
        jdbcTemplate.update("INSERT INTO patient VALUES (?, 'Test patient')", patientId);
        jdbcTemplate.update("INSERT INTO organization VALUES (?, 'Test organization')", organizationId);
        jdbcTemplate.update("INSERT INTO provider VALUES (?, ?, 'Test provider')", providerId, organizationId);
        jdbcTemplate.update("INSERT INTO coverage VALUES (?, ?, ?, '2019-01-01T00:00:00Z', '2020-01-01T00:00:00Z')", UUID.randomUUID(), patientId, planId);
        jdbcTemplate.update("INSERT INTO network_participation VALUES (?, ?, true, '2019-01-01T00:00:00Z', null)", payerId, organizationId);
        jdbcTemplate.update("INSERT INTO policy VALUES (?, 'AUTO_APPROVAL_ELIGIBLE', 'ALL')", policyId);
        jdbcTemplate.update("INSERT INTO policy_criterion VALUES (?, ?, 'OBSERVATION', 'EF', 'LTE', 35, '%')", UUID.randomUUID(), policyId);
        jdbcTemplate.update("INSERT INTO plan_service VALUES (?, ?, 'PA', 'PROCEDURE', 'COVERED', true, ?)", UUID.randomUUID(), planId, policyId);
    }

    @Test
    void persistsReviewCreationAndApprovalWithHistory() {
        UUID reviewId = createPendingReview("PA", evidence("35.0"));
        assertEquals("REVIEW_CREATED", jdbcTemplate.queryForObject("SELECT event_type FROM review_history WHERE review_id = ?", String.class, reviewId));
        assertEquals("PENDING_EVALUATION", jdbcTemplate.queryForObject("SELECT event_payload->>'reviewStatus' FROM review_history WHERE review_id = ?", String.class, reviewId));
        assertEquals("PENDING", jdbcTemplate.queryForObject("SELECT event_payload->>'requestStatus' FROM review_history WHERE review_id = ?", String.class, reviewId));
        assertEquals("PENDING_EVALUATION", jdbcTemplate.queryForObject("SELECT event_payload->>'statusReason' FROM review_history WHERE review_id = ?", String.class, reviewId));
        assertEquals(new BigDecimal("35.0"), jdbcTemplate.queryForObject("SELECT (clinical_justification->'observations'->0->>'value')::numeric FROM authorization_request WHERE id = (SELECT request_id FROM review WHERE id = ?)", BigDecimal.class, reviewId));

        reviewService.evaluateReview(reviewId);

        assertOutcome(reviewId, "DECIDED", "APPROVED", "AUTO_APPROVED");
        assertEquals(5, jdbcTemplate.queryForObject("SELECT approved_quantity FROM review WHERE id = ?", Integer.class, reviewId));
        assertEquals("DECIDED", jdbcTemplate.queryForObject("SELECT event_payload->>'reviewStatus' FROM review_history WHERE review_id = ? AND event_type = 'AUTO_APPROVED'", String.class, reviewId));
    }

    @Test
    void rollsBackRequestAndReviewWhenHistorySaveFails() {
        UUID reviewId = createPendingReview("PA", evidence("32"));
        doThrow(new IllegalStateException("History unavailable")).when(reviewHistoryRepository).save(argThat(
                (ReviewHistory history) -> "AUTO_APPROVED".equals(history.getEventType())
        ));

        assertThrows(IllegalStateException.class, () -> reviewService.evaluateReview(reviewId));

        assertEquals("PENDING_EVALUATION", jdbcTemplate.queryForObject("SELECT status FROM review WHERE id = ?", String.class, reviewId));
        assertEquals("PENDING", jdbcTemplate.queryForObject("SELECT status FROM authorization_request WHERE id = (SELECT request_id FROM review WHERE id = ?)", String.class, reviewId));
        assertEquals("PENDING_EVALUATION", jdbcTemplate.queryForObject("SELECT status_reason FROM authorization_request WHERE id = (SELECT request_id FROM review WHERE id = ?)", String.class, reviewId));
        assertNull(jdbcTemplate.queryForObject("SELECT decision FROM review WHERE id = ?", String.class, reviewId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM review_history WHERE review_id = ?", Integer.class, reviewId));
    }

    @Test
    void loadsSharedPolicyForMultipleServices() {
        jdbcTemplate.update("INSERT INTO plan_service VALUES (?, ?, 'OTHER', 'PROCEDURE', 'COVERED', true, ?)", UUID.randomUUID(), planId, policyId);
        UUID firstReviewId = createPendingReview("PA", evidence("32"));
        UUID secondReviewId = createPendingReview("OTHER", evidence("32"));

        reviewService.evaluateReview(firstReviewId);
        reviewService.evaluateReview(secondReviewId);

        assertOutcome(firstReviewId, "DECIDED", "APPROVED", "AUTO_APPROVED");
        assertOutcome(secondReviewId, "DECIDED", "APPROVED", "AUTO_APPROVED");
    }

    @AfterEach
    void deleteReferenceData() {
        jdbcTemplate.update("DELETE FROM review_history WHERE review_id IN (SELECT id FROM review WHERE request_id IN (SELECT id FROM authorization_request WHERE patient_id = ?))", patientId);
        jdbcTemplate.update("DELETE FROM review WHERE request_id IN (SELECT id FROM authorization_request WHERE patient_id = ?)", patientId);
        jdbcTemplate.update("DELETE FROM authorization_request WHERE patient_id = ?", patientId);
        jdbcTemplate.update("DELETE FROM policy_criterion WHERE policy_id = ?", policyId);
        jdbcTemplate.update("DELETE FROM plan_service WHERE plan_id = ?", planId);
        jdbcTemplate.update("DELETE FROM policy WHERE id = ?", policyId);
        jdbcTemplate.update("DELETE FROM network_participation WHERE payer_id = ?", payerId);
        jdbcTemplate.update("DELETE FROM coverage WHERE patient_id = ?", patientId);
        jdbcTemplate.update("DELETE FROM provider WHERE id = ?", providerId);
        jdbcTemplate.update("DELETE FROM patient WHERE id = ?", patientId);
        jdbcTemplate.update("DELETE FROM plan WHERE id = ?", planId);
        jdbcTemplate.update("DELETE FROM organization WHERE id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM payer WHERE id = ?", payerId);
    }

    UUID createPendingReview(String code, ClinicalJustification evidence) {
        UUID requestId = UUID.randomUUID();
        HttpAuthorizationRequest request = new HttpAuthorizationRequest(
                requestId, submittedAt, patientId, providerId, organizationId, planId,
                new RequestedService(code, null, null, Date.from(submittedAt), 5), evidence
        );
        assertEquals(ResponseStatus.SUBMITTED, authorizationRequestService.createAuthorizationRequest(request).responseStatus());
        authorizationRequestService.processSubmittedRequest(requestId);
        return jdbcTemplate.queryForObject("SELECT id FROM review WHERE request_id = ?", UUID.class, requestId);
    }

    ClinicalJustification evidence(String value) {
        return new ClinicalJustification(null, List.of(), List.of(
                new ClinicalJustification.ObservationEvidence("EF", new BigDecimal(value), "%", null, submittedAt.minusSeconds(1))
        ));
    }

    void assertOutcome(UUID reviewId, String reviewStatus, String requestStatus, String reason) {
        assertEquals(reviewStatus, jdbcTemplate.queryForObject("SELECT status FROM review WHERE id = ?", String.class, reviewId));
        assertEquals(requestStatus, jdbcTemplate.queryForObject("SELECT status FROM authorization_request WHERE id = (SELECT request_id FROM review WHERE id = ?)", String.class, reviewId));
        assertEquals(reason, jdbcTemplate.queryForObject("SELECT status_reason FROM authorization_request WHERE id = (SELECT request_id FROM review WHERE id = ?)", String.class, reviewId));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT count(*) FROM review_history WHERE review_id = ?", Integer.class, reviewId));
    }
}

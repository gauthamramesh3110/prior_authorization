package com.lifeforce.payer.review;

import com.lifeforce.payer.review.controller.ReviewController;
import com.lifeforce.payer.review.dto.ReviewHistoryEntry;
import com.lifeforce.payer.review.service.ReviewDecisionService;
import com.lifeforce.payer.review.service.ReviewEvidenceService;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewHistoryControllerTests {
    @Mock ReviewQueryService reviewQueryService;
    @Mock ReviewDecisionService reviewDecisionService;
    @Mock ReviewEvidenceService reviewEvidenceService;

    MockMvc mockMvc;
    UUID reviewId = UUID.randomUUID();

    @BeforeEach
    void createController() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewQueryService, reviewDecisionService, reviewEvidenceService)).build();
    }

    @AfterEach
    void verifiesHistoryQueriesDoNotInvokeWorkflowServices() {
        verifyNoInteractions(reviewDecisionService, reviewEvidenceService);
    }

    @Test
    void returnsHistoryEventsWithEvidenceRequestAndSubmittedEvidencePayloads() throws Exception {
        Map<String, Object> requestedPayload = new LinkedHashMap<>();
        requestedPayload.put("message", "Please provide an EF result");
        requestedPayload.put("requestedEvidence", List.of("EF result"));
        requestedPayload.put("reviewStatus", "AWAITING_EVIDENCE");
        requestedPayload.put("decision", null);
        ReviewHistoryEntry requested = new ReviewHistoryEntry(
                UUID.randomUUID(), reviewId, "REVIEWER", "EVIDENCE_REQUESTED", Instant.parse("2026-10-06T12:00:00Z"), requestedPayload
        );
        ReviewHistoryEntry submitted = new ReviewHistoryEntry(
                UUID.randomUUID(), reviewId, "PROVIDER", "UPDATED_EVIDENCE", Instant.parse("2026-10-06T12:01:00Z"),
                Map.of("clinicalJustification", Map.of("summary", "New EF result"))
        );
        when(reviewQueryService.getReviewHistory(reviewId)).thenReturn(Optional.of(List.of(requested, submitted)));

        mockMvc.perform(get("/api/v1/reviews/{id}/history", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(requested.id().toString()))
                .andExpect(jsonPath("$[0].reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$[0].eventSource").value("REVIEWER"))
                .andExpect(jsonPath("$[0].eventType").value("EVIDENCE_REQUESTED"))
                .andExpect(jsonPath("$[0].eventAt").value(requested.eventAt().toString()))
                .andExpect(jsonPath("$[0].eventPayload.message").value("Please provide an EF result"))
                .andExpect(jsonPath("$[0].eventPayload.requestedEvidence[0]").value("EF result"))
                .andExpect(jsonPath("$[0].eventPayload.reviewStatus").value("AWAITING_EVIDENCE"))
                .andExpect(jsonPath("$[0].eventPayload.decision").value(nullValue()))
                .andExpect(jsonPath("$[1].eventSource").value("PROVIDER"))
                .andExpect(jsonPath("$[1].eventType").value("UPDATED_EVIDENCE"))
                .andExpect(jsonPath("$[1].eventPayload.clinicalJustification.summary").value("New EF result"));
        verify(reviewQueryService).getReviewHistory(reviewId);
    }

    @Test
    void returnsEmptyArrayForExistingReviewWithoutHistory() throws Exception {
        when(reviewQueryService.getReviewHistory(reviewId)).thenReturn(Optional.of(List.of()));

        mockMvc.perform(get("/api/v1/reviews/{id}/history", reviewId))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void returnsNotFoundForMissingReview() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/{id}/history", reviewId))
                .andExpect(status().isNotFound());
        verify(reviewQueryService).getReviewHistory(reviewId);
    }

    @Test
    void rejectsInvalidReviewIdBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/reviews/not-a-uuid/history"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewQueryService);
    }
}

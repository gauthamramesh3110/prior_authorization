package com.lifeforce.payer.request;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import com.lifeforce.payer.request.dto.AuthorizationSubmission;
import com.lifeforce.payer.request.dto.RequestedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationRequestMappingTests {
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsSubmissionIntoDomainValuesWithOriginalRequestFields() {
        AuthorizationSubmission submission = new AuthorizationSubmission(
                UUID.randomUUID(), Instant.parse("2019-06-01T00:00:00Z"),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                objectMapper.readValue(requestedServiceJson(), RequestedService.class),
                objectMapper.readValue(clinicalJustificationJson(), ClinicalJustification.class)
        );

        AuthorizationRequest request = submission.toDomain();

        assertEquals(submission.requestId(), request.getId());
        assertEquals(submission.submittedAt(), request.getSubmittedAt());
        assertEquals(submission.patientId(), request.getPatientId());
        assertEquals(submission.providerId(), request.getProviderId());
        assertEquals(submission.organizationId(), request.getOrganizationId());
        assertEquals(submission.planId(), request.getPlanId());
        assertEquals(RequestStatus.SUBMITTED, request.getRequestStatus());
        assertNull(request.getRequestStatusReason());
        assertNull(request.getEvidenceUpdatedAt());
        assertEquals(submission.requestedService(), RequestedService.from(request.getRequestedService()));
        assertEquals(submission.clinicalJustification(), ClinicalJustification.from(request.getClinicalJustification()));
    }

    @Test
    void readsStoredRequestedServiceJsonAndKeepsApiAndDomainJsonEquivalent() {
        RequestedService expected = objectMapper.readValue(requestedServiceJson(), RequestedService.class);
        var storedService = objectMapper.readValue(requestedServiceJson(), com.lifeforce.payer.request.domain.RequestedService.class);

        assertEquals(expected, RequestedService.from(storedService));
        assertEquals(storedService, expected.toDomain());
        assertEquals(objectMapper.valueToTree(expected), objectMapper.valueToTree(storedService));
    }

    @Test
    void readsStoredClinicalJsonAndPreservesNestedEvidenceFieldsAndJsonShape() {
        ClinicalJustification expected = objectMapper.readValue(clinicalJustificationJson(), ClinicalJustification.class);
        var storedEvidence = objectMapper.readValue(clinicalJustificationJson(), com.lifeforce.payer.request.domain.ClinicalJustification.class);

        assertEquals(expected, ClinicalJustification.from(storedEvidence));
        assertEquals(storedEvidence, expected.toDomain());
        assertEquals(objectMapper.valueToTree(expected), objectMapper.valueToTree(storedEvidence));
        assertEquals(expected.conditions().getFirst().startDate(), storedEvidence.conditions().getFirst().startDate());
        assertEquals(expected.conditions().getFirst().endDate(), storedEvidence.conditions().getFirst().endDate());
        assertEquals(expected.observations().getFirst().recordedAt(), storedEvidence.observations().getFirst().recordedAt());
        assertEquals(expected.observations().getFirst().value(), storedEvidence.observations().getFirst().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]"})
    void preservesAbsentAndEmptyEvidenceLists(String lists) {
        ClinicalJustification justification = objectMapper.readValue(
                "{\"summary\":null,\"conditions\":%s,\"observations\":%s}".formatted(lists, lists),
                ClinicalJustification.class
        );

        var domainEvidence = justification.toDomain();

        assertEquals(justification, ClinicalJustification.from(domainEvidence));
        assertEquals(objectMapper.valueToTree(justification), objectMapper.valueToTree(domainEvidence));
    }

    @Test
    void keepsMissingClinicalJustificationAbsentInApiResponse() {
        assertNull(ClinicalJustification.from(null));
    }

    String requestedServiceJson() {
        return """
                {"code":"93306","codeSystem":"CPT","description":"Echocardiogram",
                 "requestedDate":"2019-06-01T00:00:00Z","quantity":5}
                """;
    }

    String clinicalJustificationJson() {
        return """
                {"summary":"Clinical summary",
                 "conditions":[{"code":"CHF","description":"Heart failure",
                                "startDate":"2019-01-01T00:00:00Z","endDate":"2020-01-01T00:00:00Z"}],
                 "observations":[{"code":"EF","value":35.1,"units":"%",
                                  "description":"Ejection fraction","recordedAt":"2019-05-31T12:00:00Z"}]}
                """;
    }
}

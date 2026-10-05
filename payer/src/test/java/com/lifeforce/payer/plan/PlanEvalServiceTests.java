package com.lifeforce.payer.plan;

import com.lifeforce.payer.plan.domain.policy.*;
import com.lifeforce.payer.plan.service.PlanEvalService;
import com.lifeforce.payer.plan.service.PolicyEvaluationResult;
import com.lifeforce.payer.request.dto.ClinicalJustification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanEvalServiceTests {
    PlanEvalService planEvalService = new PlanEvalService();
    Instant submittedAt = Instant.parse("2019-06-01T00:00:00Z");

    @ParameterizedTest
    @CsvSource({
            "2019-05-01T00:00:00Z, MATCHED",
            "2019-06-01T00:00:00Z, MATCHED",
            "2019-07-01T00:00:00Z, AWAITING_EVIDENCE"
    })
    void checksConditionStartAtSubmission(String start, PolicyEvaluationResult expected) {
        ClinicalJustification evidence = new ClinicalJustification(null, List.of(
                new ClinicalJustification.ConditionEvidence("CHF", null, Date.from(Instant.parse(start)), null)
        ), List.of());

        assertEquals(expected, planEvalService.evalPolicyForEvidence(conditionPolicy(), evidence, submittedAt));
    }

    @ParameterizedTest
    @CsvSource({
            "2019-05-01T00:00:00Z, AWAITING_EVIDENCE",
            "2019-06-01T00:00:00Z, AWAITING_EVIDENCE",
            "2019-07-01T00:00:00Z, MATCHED"
    })
    void checksConditionEndAtSubmission(String end, PolicyEvaluationResult expected) {
        ClinicalJustification evidence = new ClinicalJustification(null, List.of(
                new ClinicalJustification.ConditionEvidence("CHF", null, Date.from(submittedAt.minusSeconds(100)), Date.from(Instant.parse(end)))
        ), List.of());

        assertEquals(expected, planEvalService.evalPolicyForEvidence(conditionPolicy(), evidence, submittedAt));
    }

    @ParameterizedTest
    @CsvSource({
            "EQ, 35.0, MATCHED", "EQ, 35.1, CRITERIA_NOT_MET",
            "GT, 35, CRITERIA_NOT_MET", "GT, 35.1, MATCHED",
            "GTE, 35, MATCHED", "GTE, 34.9, CRITERIA_NOT_MET",
            "LT, 35, CRITERIA_NOT_MET", "LT, 34.9, MATCHED",
            "LTE, 35, MATCHED", "LTE, 35.1, CRITERIA_NOT_MET"
    })
    void comparesNumericBoundaries(PolicyCriterionOperator operator, String value, PolicyEvaluationResult expected) {
        Policy policy = policy(Match.ALL, observationCriterion(operator));
        ClinicalJustification evidence = new ClinicalJustification(null, List.of(), List.of(observation(value, "%", submittedAt)));

        assertEquals(expected, planEvalService.evalPolicyForEvidence(policy, evidence, submittedAt));
    }

    @Test
    void usesLatestEligibleObservation() {
        ClinicalJustification evidence = new ClinicalJustification(null, List.of(), List.of(
                observation("32", "%", submittedAt.minusSeconds(100)),
                observation("40", "%", submittedAt.minusSeconds(10)),
                observation("30", "%", submittedAt.plusSeconds(10))
        ));

        assertEquals(PolicyEvaluationResult.CRITERIA_NOT_MET, planEvalService.evalPolicyForEvidence(observationPolicy(), evidence, submittedAt));
    }

    @Test
    void waitsForEvidenceWhenOnlyObservationIsAfterSubmission() {
        ClinicalJustification evidence = new ClinicalJustification(null, List.of(), List.of(observation("32", "%", submittedAt.plusSeconds(1))));

        assertEquals(PolicyEvaluationResult.AWAITING_EVIDENCE, planEvalService.evalPolicyForEvidence(observationPolicy(), evidence, submittedAt));
    }

    @Test
    void routesWrongUnitsAndConflictingLatestObservationsToManualReview() {
        ClinicalJustification wrongUnit = new ClinicalJustification(null, List.of(), List.of(observation("32", "mL", submittedAt)));
        ClinicalJustification conflicting = new ClinicalJustification(null, List.of(), List.of(
                observation("32", "%", submittedAt), observation("40", "%", submittedAt)
        ));
        ClinicalJustification conflictingUnits = new ClinicalJustification(null, List.of(), List.of(
                observation("32", "%", submittedAt), observation("32", "mL", submittedAt)
        ));

        for (ClinicalJustification evidence : List.of(wrongUnit, conflicting, conflictingUnits)) {
            assertEquals(PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED, planEvalService.evalPolicyForEvidence(observationPolicy(), evidence, submittedAt));
        }
    }

    @Test
    void acceptsEquivalentLatestMeasurements() {
        ClinicalJustification evidence = new ClinicalJustification(null, List.of(), List.of(
                observation("35", "%", submittedAt), observation("35.0", "%", submittedAt)
        ));

        assertEquals(PolicyEvaluationResult.MATCHED, planEvalService.evalPolicyForEvidence(observationPolicy(), evidence, submittedAt));
    }

    @Test
    void handlesAllAndAnyWithMissingAndFailedCriteria() {
        PolicyCriterion condition = conditionCriterion();
        PolicyCriterion observation = observationCriterion(PolicyCriterionOperator.LTE);
        ClinicalJustification missingCondition = new ClinicalJustification(null, List.of(), List.of(observation("32", "%", submittedAt)));
        ClinicalJustification failedObservation = new ClinicalJustification(null, conditions(), List.of(observation("40", "%", submittedAt)));
        ClinicalJustification matched = new ClinicalJustification(null, conditions(), List.of(observation("35", "%", submittedAt)));

        assertEquals(PolicyEvaluationResult.MATCHED, planEvalService.evalPolicyForEvidence(policy(Match.ANY, condition, observation), missingCondition, submittedAt));
        assertEquals(PolicyEvaluationResult.AWAITING_EVIDENCE, planEvalService.evalPolicyForEvidence(policy(Match.ALL, condition, observation), missingCondition, submittedAt));
        assertEquals(PolicyEvaluationResult.CRITERIA_NOT_MET, planEvalService.evalPolicyForEvidence(policy(Match.ALL, condition, observation), failedObservation, submittedAt));
        assertEquals(PolicyEvaluationResult.MATCHED, planEvalService.evalPolicyForEvidence(policy(Match.ALL, condition, observation), matched, submittedAt));
    }

    @Test
    void handlesAbsentEvidenceAndNullLists() {
        assertEquals(PolicyEvaluationResult.AWAITING_EVIDENCE, planEvalService.evalPolicyForEvidence(conditionPolicy(), null, submittedAt));
        assertEquals(PolicyEvaluationResult.AWAITING_EVIDENCE, planEvalService.evalPolicyForEvidence(conditionPolicy(), new ClinicalJustification(null, null, null), submittedAt));
        assertEquals(PolicyEvaluationResult.AWAITING_EVIDENCE, planEvalService.evalPolicyForEvidence(observationPolicy(), new ClinicalJustification(null, null, null), submittedAt));
    }

    @Test
    void routesMissingEmptyManualAndInvalidPoliciesToManualReview() {
        ClinicalJustification evidence = new ClinicalJustification(null, conditions(), List.of());
        Policy manual = conditionPolicy();
        ReflectionTestUtils.setField(manual, "reviewMode", ReviewMode.MANUAL_REVIEW);
        Policy invalid = policy(Match.ALL, observationCriterion(PolicyCriterionOperator.PRESENT));

        assertEquals(PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED, planEvalService.evalPolicyForEvidence(null, evidence, submittedAt));
        for (Policy policy : List.of(policy(Match.ALL), policy(Match.ANY), manual, invalid)) {
            assertEquals(PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED, planEvalService.evalPolicyForEvidence(policy, evidence, submittedAt));
        }
    }

    Policy conditionPolicy() {
        return policy(Match.ANY, conditionCriterion());
    }

    Policy observationPolicy() {
        return policy(Match.ALL, observationCriterion(PolicyCriterionOperator.LTE));
    }

    Policy policy(Match match, PolicyCriterion... criteria) {
        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "match", match);
        ReflectionTestUtils.setField(policy, "reviewMode", ReviewMode.AUTO_APPROVAL_ELIGIBLE);
        ReflectionTestUtils.setField(policy, "policyCriteria", List.of(criteria));
        return policy;
    }

    PolicyCriterion conditionCriterion() {
        PolicyCriterion criterion = new PolicyCriterion();
        ReflectionTestUtils.setField(criterion, "evidenceType", EvidenceType.CONDITION);
        ReflectionTestUtils.setField(criterion, "code", "CHF");
        ReflectionTestUtils.setField(criterion, "operator", PolicyCriterionOperator.PRESENT);
        return criterion;
    }

    PolicyCriterion observationCriterion(PolicyCriterionOperator operator) {
        PolicyCriterion criterion = new PolicyCriterion();
        ReflectionTestUtils.setField(criterion, "evidenceType", EvidenceType.OBSERVATION);
        ReflectionTestUtils.setField(criterion, "code", "EF");
        ReflectionTestUtils.setField(criterion, "operator", operator);
        ReflectionTestUtils.setField(criterion, "value", new BigDecimal("35"));
        ReflectionTestUtils.setField(criterion, "unit", "%");
        return criterion;
    }

    List<ClinicalJustification.ConditionEvidence> conditions() {
        return List.of(new ClinicalJustification.ConditionEvidence("CHF", null, Date.from(submittedAt.minusSeconds(100)), null));
    }

    ClinicalJustification.ObservationEvidence observation(String value, String unit, Instant recordedAt) {
        return new ClinicalJustification.ObservationEvidence("EF", new BigDecimal(value), unit, null, recordedAt);
    }
}

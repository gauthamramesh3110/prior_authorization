package com.lifeforce.payer.plan.service;

import com.lifeforce.payer.plan.domain.policy.*;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PlanEvalService {

    public PlanEvalService() {
    }

    public PolicyEvaluationResult evalPolicyForEvidence(Policy policy, ClinicalJustification justification, Instant evidenceAt) {
        if (policy == null || policy.getReviewMode() != ReviewMode.AUTO_APPROVAL_ELIGIBLE || policy.getMatch() == null || policy.getPolicyCriteria() == null || policy.getPolicyCriteria().isEmpty()) {
            return PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED;
        }

        List<PolicyEvaluationResult> results = new ArrayList<>();
        for (PolicyCriterion criterion : policy.getPolicyCriteria()) {
            results.add(evalCriterionForEvidence(criterion, justification, evidenceAt));
        }

        if (results.contains(PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED)) {
            return PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED;
        }
        if (policy.getMatch().equals(Match.ANY) && results.contains(PolicyEvaluationResult.MATCHED)) {
            return PolicyEvaluationResult.MATCHED;
        }
        if (policy.getMatch().equals(Match.ALL) && results.contains(PolicyEvaluationResult.CRITERIA_NOT_MET)) {
            return PolicyEvaluationResult.CRITERIA_NOT_MET;
        }
        if (results.contains(PolicyEvaluationResult.AWAITING_EVIDENCE)) {
            return PolicyEvaluationResult.AWAITING_EVIDENCE;
        }

        return policy.getMatch().equals(Match.ALL) ? PolicyEvaluationResult.MATCHED : PolicyEvaluationResult.CRITERIA_NOT_MET;
    }

    private PolicyEvaluationResult evalCriterionForEvidence(PolicyCriterion criterion, ClinicalJustification justification, Instant evidenceAt) {
        if (justification == null) {
            return PolicyEvaluationResult.AWAITING_EVIDENCE;
        }
        if (criterion.getEvidenceType() == EvidenceType.CONDITION) {
            if (criterion.getOperator() != PolicyCriterionOperator.PRESENT) {
                return PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED;
            }
            if (justification.conditions() == null) {
                return PolicyEvaluationResult.AWAITING_EVIDENCE;
            }

            boolean conditionPresent = justification.conditions().stream().anyMatch(conditionEvidence -> (
                    conditionEvidence.code().equals(criterion.getCode()) &&
                    !conditionEvidence.startDate().after(Date.from(evidenceAt)) &&
                    (conditionEvidence.endDate() == null || conditionEvidence.endDate().after(Date.from(evidenceAt)))
            ));
            return conditionPresent ? PolicyEvaluationResult.MATCHED : PolicyEvaluationResult.AWAITING_EVIDENCE;
        }
        if (criterion.getEvidenceType() != EvidenceType.OBSERVATION || criterion.getOperator() == null || criterion.getOperator() == PolicyCriterionOperator.PRESENT || criterion.getValue() == null || criterion.getUnit() == null) {
            return PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED;
        }
        if (justification.observations() == null) {
            return PolicyEvaluationResult.AWAITING_EVIDENCE;
        }

        List<ClinicalJustification.ObservationEvidence> observations = justification.observations().stream().filter(observationEvidence -> (
                observationEvidence.code().equals(criterion.getCode()) &&
                !observationEvidence.recordedAt().isAfter(evidenceAt)
        )).toList();
        Optional<ClinicalJustification.ObservationEvidence> latestObservation = observations.stream().max(Comparator.comparing(ClinicalJustification.ObservationEvidence::recordedAt));
        if (latestObservation.isEmpty()) {
            return PolicyEvaluationResult.AWAITING_EVIDENCE;
        }

        ClinicalJustification.ObservationEvidence observation = latestObservation.get();
        boolean hasConflictingObservations = observations.stream().anyMatch(observationEvidence -> (
                observationEvidence.recordedAt().equals(observation.recordedAt()) &&
                (!observationEvidence.units().equals(observation.units()) || observationEvidence.value().compareTo(observation.value()) != 0)
        ));
        if (hasConflictingObservations || !criterion.getUnit().equals(observation.units())) {
            return PolicyEvaluationResult.MANUAL_REVIEW_REQUIRED;
        }

        int comparison = observation.value().compareTo(criterion.getValue());
        boolean criterionMet = switch (criterion.getOperator()) {
            case EQ -> comparison == 0;
            case GT -> comparison > 0;
            case GTE -> comparison >= 0;
            case LT -> comparison < 0;
            case LTE -> comparison <= 0;
            default -> false;
        };
        return criterionMet ? PolicyEvaluationResult.MATCHED : PolicyEvaluationResult.CRITERIA_NOT_MET;
    }
}

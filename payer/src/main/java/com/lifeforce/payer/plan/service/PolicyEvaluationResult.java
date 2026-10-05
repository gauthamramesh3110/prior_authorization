package com.lifeforce.payer.plan.service;

public enum PolicyEvaluationResult {
    MATCHED,
    CRITERIA_NOT_MET,
    AWAITING_EVIDENCE,
    MANUAL_REVIEW_REQUIRED
}

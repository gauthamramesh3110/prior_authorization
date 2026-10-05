package com.lifeforce.payer.plan.domain.policy;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity(name = "policy_criterion")
public class PolicyCriterion {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private EvidenceType evidenceType;

    private String code;

    @Enumerated(EnumType.STRING)
    private PolicyCriterionOperator operator;

    private BigDecimal value;

    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;
}

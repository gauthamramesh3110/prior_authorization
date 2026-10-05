package com.lifeforce.payer.plan.domain.plan;

import com.lifeforce.payer.plan.domain.policy.Policy;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity(name = "plan_service")
public class PlanService {
    @Id
    private UUID id;

    @Column(name = "plan_id")
    private UUID planId;

    private String code;

    @Enumerated(EnumType.STRING)
    private CodeType codeType;

    @Enumerated(EnumType.STRING)
    private BenefitStatus benefitStatus;

    private Boolean priorAuthorizationRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false,  nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;
}

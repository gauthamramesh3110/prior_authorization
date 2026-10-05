package com.lifeforce.payer.plan.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity(name = "plan_service")
public class PlanService {
    @Id
    private UUID id;

    private String code;

    @Enumerated(EnumType.STRING)
    private CodeType codeType;

    @Enumerated(EnumType.STRING)
    private BenefitStatus benefitStatus;

    private Boolean priorAuthorizationRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id",  nullable = false)
    private Plan plan;
}

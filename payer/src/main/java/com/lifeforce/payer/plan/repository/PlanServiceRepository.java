package com.lifeforce.payer.plan.repository;

import com.lifeforce.payer.plan.domain.plan.CodeType;
import com.lifeforce.payer.plan.domain.plan.PlanService;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PlanServiceRepository extends Repository<PlanService, UUID> {
    Optional<PlanService> findByPlanIdAndCodeAndCodeType(UUID planId, String code, CodeType codeType);
}

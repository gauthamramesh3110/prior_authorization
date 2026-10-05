package com.lifeforce.payer.plan.repository;

import com.lifeforce.payer.plan.domain.plan.Plan;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends Repository<Plan, UUID> {
    Optional<Plan> findById(UUID id);

    boolean existsById(UUID id);
}

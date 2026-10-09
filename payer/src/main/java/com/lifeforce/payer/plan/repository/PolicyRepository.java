package com.lifeforce.payer.plan.repository;

import com.lifeforce.payer.plan.domain.policy.Policy;
import com.lifeforce.payer.plan.domain.policy.IngestionStatus;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends Repository<Policy, UUID> {
    Optional<Policy> findById(UUID id);

    List<Policy> findByIngestionStatusOrderByIdAsc(IngestionStatus ingestionStatus);

    Policy save(Policy policy);
}

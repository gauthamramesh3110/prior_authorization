package com.lifeforce.payer.reference.repository;

import com.lifeforce.payer.reference.domain.Provider;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface ProviderRepository extends Repository<Provider, UUID> {
    boolean existsById(UUID id);

    boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);
}

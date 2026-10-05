package com.lifeforce.payer.reference.repository;

import com.lifeforce.payer.reference.domain.Organization;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface OrganizationRepository extends Repository<Organization, UUID> {
    boolean existsById(UUID id);
}

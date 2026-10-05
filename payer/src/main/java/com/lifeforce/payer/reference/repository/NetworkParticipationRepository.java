package com.lifeforce.payer.reference.repository;

import com.lifeforce.payer.reference.domain.NetworkParticipation;
import com.lifeforce.payer.reference.domain.NetworkParticipationId;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface NetworkParticipationRepository extends Repository<NetworkParticipation, NetworkParticipationId> {
    List<NetworkParticipation> findByPayerIdAndOrganizationId(UUID payerId, UUID organizationId);
}

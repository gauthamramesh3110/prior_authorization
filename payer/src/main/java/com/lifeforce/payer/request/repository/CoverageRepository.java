package com.lifeforce.payer.request.repository;

import com.lifeforce.payer.request.domain.Coverage;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoverageRepository extends Repository<Coverage, UUID> {
    List<Coverage> findByPatientIdAndPlanId(UUID patientId, UUID planId);
}

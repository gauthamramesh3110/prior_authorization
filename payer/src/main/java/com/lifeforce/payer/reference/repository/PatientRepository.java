package com.lifeforce.payer.reference.repository;

import com.lifeforce.payer.reference.domain.Patient;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface PatientRepository extends Repository<Patient, UUID> {
    boolean existsById(UUID id);
}

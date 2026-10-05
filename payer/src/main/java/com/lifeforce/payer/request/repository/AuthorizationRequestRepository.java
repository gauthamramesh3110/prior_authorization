package com.lifeforce.payer.request.repository;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.Status;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationRequestRepository extends Repository<AuthorizationRequest, UUID> {
    boolean existsById(UUID id);

    List<AuthorizationRequest> findByStatus(Status status);

    AuthorizationRequest save(AuthorizationRequest request);

    Optional<AuthorizationRequest> findById(UUID id);
}

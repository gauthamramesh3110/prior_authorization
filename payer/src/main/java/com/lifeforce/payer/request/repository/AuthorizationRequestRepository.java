package com.lifeforce.payer.request.repository;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AuthorizationRequestRepository extends Repository<AuthorizationRequest, UUID> {
    boolean existsById(UUID id);

    AuthorizationRequest save(AuthorizationRequest request);
}

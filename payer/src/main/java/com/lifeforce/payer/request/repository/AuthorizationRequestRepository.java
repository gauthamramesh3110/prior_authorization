package com.lifeforce.payer.request.repository;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationRequestRepository extends Repository<AuthorizationRequest, UUID> {
    boolean existsById(UUID id);

    List<AuthorizationRequest> findByRequestStatus(RequestStatus requestStatus);

    List<AuthorizationRequest> findByProviderIdOrderBySubmittedAtDescIdAsc(UUID providerId);

    List<AuthorizationRequest> findByProviderIdAndRequestStatusOrderBySubmittedAtDescIdAsc(UUID providerId, RequestStatus status);

    AuthorizationRequest save(AuthorizationRequest request);

    Optional<AuthorizationRequest> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from authorization_request request where request.id = :id")
    Optional<AuthorizationRequest> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from authorization_request request where request.id = (select review.requestId from review review where review.id = :reviewId)")
    Optional<AuthorizationRequest> findByReviewIdForUpdate(@Param("reviewId") UUID reviewId);
}

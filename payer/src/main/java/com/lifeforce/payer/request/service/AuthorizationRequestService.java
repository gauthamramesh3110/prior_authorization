package com.lifeforce.payer.request.service;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.dto.HttpAuthorizationRequest;
import com.lifeforce.payer.request.dto.HttpAuthorizationResponse;
import com.lifeforce.payer.request.dto.ResponseStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationRequestService {
    private final AuthorizationRequestRepository authorizationRequestRepository;
    public AuthorizationRequestService(AuthorizationRequestRepository authorizationRequestRepository) {
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Transactional
    public HttpAuthorizationResponse createAuthorizationRequest(HttpAuthorizationRequest request) {
        boolean isDuplicate = authorizationRequestRepository.existsById(request.requestId());
        if (isDuplicate) {
            return new HttpAuthorizationResponse(
                    request.requestId(),
                    ResponseStatus.DUPLICATE_REJECTED,
                    "Request has already been submitted"
            );
        }

        AuthorizationRequest authorizationRequest = new AuthorizationRequest().build(request);
        authorizationRequestRepository.save(authorizationRequest);

        return new HttpAuthorizationResponse(
                authorizationRequest.getId(),
                ResponseStatus.SUBMITTED,
                "Request has been submitted"
        );
    }

}

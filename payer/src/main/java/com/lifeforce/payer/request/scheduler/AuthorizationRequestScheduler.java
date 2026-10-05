package com.lifeforce.payer.request.scheduler;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.Status;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationRequestScheduler {

    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final AuthorizationRequestService authorizationRequestService;
    public AuthorizationRequestScheduler(AuthorizationRequestRepository authorizationRequestRepository, AuthorizationRequestService authorizationRequestService) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.authorizationRequestService = authorizationRequestService;
    }

    @Scheduled(fixedRate = 60000,  initialDelay = 60000)
    public void processSubmittedRequests() {
        List<AuthorizationRequest> submittedRequests = authorizationRequestRepository.findByStatus(Status.SUBMITTED);

        for (AuthorizationRequest request : submittedRequests) {
            authorizationRequestService.processSubmittedRequest(request.getId());
        }
    }
}

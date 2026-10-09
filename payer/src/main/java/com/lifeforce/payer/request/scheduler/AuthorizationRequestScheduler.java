package com.lifeforce.payer.request.scheduler;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationRequestScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationRequestScheduler.class);

    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final AuthorizationRequestService authorizationRequestService;
    public AuthorizationRequestScheduler(AuthorizationRequestRepository authorizationRequestRepository, AuthorizationRequestService authorizationRequestService) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.authorizationRequestService = authorizationRequestService;
    }

    @Scheduled(fixedDelayString = "${payer.scheduler.request-delay}", initialDelayString = "${payer.scheduler.request-delay}")
    public void processSubmittedRequests() {
        List<AuthorizationRequest> submittedRequests = authorizationRequestRepository.findByRequestStatus(RequestStatus.SUBMITTED);

        for (AuthorizationRequest request : submittedRequests) {
            try {
                authorizationRequestService.processSubmittedRequest(request.getId());
            } catch (Exception exception) {
                logger.error("Failed to process request {}", request.getId(), exception);
            }
        }
    }
}

package com.lifeforce.payer.request;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.request.scheduler.AuthorizationRequestScheduler;
import com.lifeforce.payer.request.service.AuthorizationRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestSchedulerTests {
    @Mock AuthorizationRequestRepository authorizationRequestRepository;
    @Mock AuthorizationRequestService authorizationRequestService;

    @Test
    void sendsSubmittedRequestIdsToService() {
        AuthorizationRequest firstRequest = request();
        AuthorizationRequest secondRequest = request();
        when(authorizationRequestRepository.findByRequestStatus(RequestStatus.SUBMITTED)).thenReturn(List.of(firstRequest, secondRequest));
        AuthorizationRequestScheduler scheduler = new AuthorizationRequestScheduler(authorizationRequestRepository, authorizationRequestService);

        scheduler.processSubmittedRequests();

        verify(authorizationRequestService).processSubmittedRequest(firstRequest.getId());
        verify(authorizationRequestService).processSubmittedRequest(secondRequest.getId());
        verifyNoMoreInteractions(authorizationRequestService);
    }

    @Test
    void doesNotCallServiceWhenNoSubmittedRequestsExist() {
        AuthorizationRequestScheduler scheduler = new AuthorizationRequestScheduler(authorizationRequestRepository, authorizationRequestService);

        scheduler.processSubmittedRequests();

        verify(authorizationRequestRepository).findByRequestStatus(RequestStatus.SUBMITTED);
        verifyNoInteractions(authorizationRequestService);
    }

    @Test
    void continuesProcessingWhenOneRequestFails() {
        AuthorizationRequest firstRequest = request();
        AuthorizationRequest secondRequest = request();
        when(authorizationRequestRepository.findByRequestStatus(RequestStatus.SUBMITTED)).thenReturn(List.of(firstRequest, secondRequest));
        doThrow(new IllegalStateException("Request failed")).when(authorizationRequestService).processSubmittedRequest(firstRequest.getId());
        AuthorizationRequestScheduler scheduler = new AuthorizationRequestScheduler(authorizationRequestRepository, authorizationRequestService);

        scheduler.processSubmittedRequests();

        var orderedCalls = inOrder(authorizationRequestService);
        orderedCalls.verify(authorizationRequestService).processSubmittedRequest(firstRequest.getId());
        orderedCalls.verify(authorizationRequestService).processSubmittedRequest(secondRequest.getId());
        verifyNoMoreInteractions(authorizationRequestService);
    }

    AuthorizationRequest request() {
        AuthorizationRequest request = new AuthorizationRequest();
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        return request;
    }
}

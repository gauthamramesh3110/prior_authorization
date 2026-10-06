package com.lifeforce.payer.request.service;

import com.lifeforce.payer.reference.repository.ProviderRepository;
import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.dto.RequestDetails;
import com.lifeforce.payer.request.dto.RequestSummary;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RequestQueryService {
    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ProviderRepository providerRepository;

    public RequestQueryService(AuthorizationRequestRepository authorizationRequestRepository, ReviewRepository reviewRepository, ProviderRepository providerRepository) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.reviewRepository = reviewRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<RequestSummary> getProviderRequests(UUID providerId, RequestStatus requestStatus) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider does not exist");
        }
        List<AuthorizationRequest> requests = requestStatus == null
                ? authorizationRequestRepository.findByProviderIdOrderBySubmittedAtDescIdAsc(providerId)
                : authorizationRequestRepository.findByProviderIdAndRequestStatusOrderBySubmittedAtDescIdAsc(providerId, requestStatus);
        if (requests.isEmpty()) {
            return List.of();
        }
        List<UUID> requestIds = requests.stream().map(AuthorizationRequest::getId).toList();
        Map<UUID, Review> reviews = reviewRepository.findByRequestIdIn(requestIds).stream()
                .collect(Collectors.toMap(Review::getRequestId, Function.identity()));
        return requests.stream().map(request -> RequestSummary.from(request, reviews.get(request.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public RequestDetails getProviderRequestDetails(UUID requestId, UUID providerId) {
        AuthorizationRequest request = authorizationRequestRepository.findById(requestId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authorization request does not exist")
        );
        if (!request.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider does not match the authorization request");
        }
        Review review = reviewRepository.findByRequestId(requestId).orElse(null);
        return RequestDetails.from(request, review);
    }
}

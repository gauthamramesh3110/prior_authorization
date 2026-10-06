package com.lifeforce.payer.review.service;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Decision;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ManualDecisionRequest;
import com.lifeforce.payer.review.dto.ReviewDecisionResponse;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
public class ReviewDecisionService {
    private final ReviewRepository reviewRepository;
    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final Clock clock;

    public ReviewDecisionService(ReviewRepository reviewRepository, AuthorizationRequestRepository authorizationRequestRepository, ReviewHistoryRepository reviewHistoryRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.clock = clock;
    }

    @Transactional
    public ReviewDecisionResponse submitManualDecision(UUID reviewId, ManualDecisionRequest decisionRequest) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review does not exist")
        );
        AuthorizationRequest request = review.getAuthorizationRequest();
        if (review.getReviewStatus() != ReviewStatus.PENDING_MANUAL_REVIEW || request.getRequestStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is not pending manual review");
        }

        if (decisionRequest.decision() == Decision.APPROVED) {
            Integer quantity = decisionRequest.approvedQuantity();
            if (quantity == null || quantity <= 0 || quantity > request.getRequestedService().quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved quantity must be positive and no greater than the requested quantity");
            }
            request.updateStatusToManuallyApproved();
        } else {
            if (decisionRequest.approvedQuantity() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejected reviews must not have an approved quantity");
            }
            request.updateStatusToManuallyRejected();
        }

        review.updateStatusToManuallyDecided(decisionRequest.decision(), decisionRequest.decisionReason(), decisionRequest.reviewerId(), decisionRequest.approvedQuantity(), clock);
        authorizationRequestRepository.save(request);
        reviewRepository.save(review);
        reviewHistoryRepository.save(ReviewHistory.createReviewerEvent(review));
        return ReviewDecisionResponse.from(review);
    }
}

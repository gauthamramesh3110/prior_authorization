package com.lifeforce.payer.review.service;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewHistory;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.EvidenceRequest;
import com.lifeforce.payer.review.dto.EvidenceRequestResponse;
import com.lifeforce.payer.review.repository.ReviewHistoryRepository;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
public class ReviewEvidenceService {
    private final ReviewRepository reviewRepository;
    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final Clock clock;

    public ReviewEvidenceService(ReviewRepository reviewRepository, AuthorizationRequestRepository authorizationRequestRepository, ReviewHistoryRepository reviewHistoryRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.clock = clock;
    }

    @Transactional
    public EvidenceRequestResponse requestEvidence(UUID reviewId, EvidenceRequest evidenceRequest) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review does not exist")
        );
        AuthorizationRequest request = review.getAuthorizationRequest();
        if ((review.getReviewStatus() != ReviewStatus.PENDING_MANUAL_REVIEW && review.getReviewStatus() != ReviewStatus.AWAITING_EVIDENCE)
                || request.getRequestStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is not pending manual review or awaiting evidence");
        }

        request.updateStatusToAwaitingEvidence();
        review.requestEvidence(evidenceRequest.reviewerId(), evidenceRequest.evidenceRequest().toDomain(), clock);
        ReviewHistory history = ReviewHistory.createEvidenceRequestedEvent(review);
        authorizationRequestRepository.save(request);
        reviewRepository.save(review);
        reviewHistoryRepository.save(history);
        return EvidenceRequestResponse.from(review, history);
    }
}

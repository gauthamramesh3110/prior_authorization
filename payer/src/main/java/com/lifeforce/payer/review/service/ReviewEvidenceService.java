package com.lifeforce.payer.review.service;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.EvidenceRequest;
import com.lifeforce.payer.review.dto.EvidenceRequestResponse;
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
    private final Clock clock;

    public ReviewEvidenceService(ReviewRepository reviewRepository, AuthorizationRequestRepository authorizationRequestRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.clock = clock;
    }

    @Transactional
    public EvidenceRequestResponse requestEvidence(UUID reviewId, EvidenceRequest evidenceRequest) {
        AuthorizationRequest request = authorizationRequestRepository.findByReviewIdForUpdate(reviewId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review does not exist")
        );
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review does not exist")
        );
        if ((review.getReviewStatus() != ReviewStatus.PENDING_MANUAL_REVIEW && review.getReviewStatus() != ReviewStatus.AWAITING_EVIDENCE)
                || request.getRequestStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is not pending manual review or awaiting evidence");
        }

        request.updateStatusToAwaitingEvidence();
        review.requestEvidence(evidenceRequest.reviewerId(), evidenceRequest.evidenceRequest().toDomain(), clock);
        authorizationRequestRepository.save(request);
        reviewRepository.save(review);
        return EvidenceRequestResponse.from(review);
    }
}

package com.lifeforce.payer.request.service;

import com.lifeforce.payer.request.domain.AuthorizationRequest;
import com.lifeforce.payer.request.domain.RequestStatus;
import com.lifeforce.payer.request.domain.ClinicalJustification;
import com.lifeforce.payer.request.dto.EvidenceSubmission;
import com.lifeforce.payer.request.dto.EvidenceSubmissionResponse;
import com.lifeforce.payer.request.repository.AuthorizationRequestRepository;
import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
public class EvidenceSubmissionService {
    private final AuthorizationRequestRepository authorizationRequestRepository;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    public EvidenceSubmissionService(AuthorizationRequestRepository authorizationRequestRepository, ReviewRepository reviewRepository, Clock clock) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.reviewRepository = reviewRepository;
        this.clock = clock;
    }

    @Transactional
    public EvidenceSubmissionResponse submitEvidence(UUID requestId, EvidenceSubmission submission) {
        AuthorizationRequest request = authorizationRequestRepository.findById(requestId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authorization request does not exist")
        );
        if (!request.getProviderId().equals(submission.providerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider does not match the authorization request");
        }
        Review review = reviewRepository.findByRequestId(requestId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.CONFLICT, "Authorization request is not awaiting evidence")
        );
        if (review.getReviewStatus() != ReviewStatus.AWAITING_EVIDENCE || request.getRequestStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Authorization request is not awaiting evidence");
        }

        ClinicalJustification evidence = submission.clinicalJustification().toDomain();
        if ((evidence.summary() == null || evidence.summary().isBlank())
                && (evidence.conditions() == null || evidence.conditions().isEmpty())
                && (evidence.observations() == null || evidence.observations().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one evidence item or a nonblank summary is required");
        }

        request.addEvidence(evidence, clock);
        if (review.getReviewerId() != null) {
            review.updateStatusToManualReview(clock);
        } else {
            review.updateStatusToPendingEvaluation(clock);
        }
        authorizationRequestRepository.save(request);
        reviewRepository.save(review);
        return EvidenceSubmissionResponse.from(review);
    }
}

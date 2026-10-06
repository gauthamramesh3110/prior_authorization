package com.lifeforce.payer.review.controller;

import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.EvidenceRequest;
import com.lifeforce.payer.review.dto.EvidenceRequestResponse;
import com.lifeforce.payer.review.dto.ManualDecisionRequest;
import com.lifeforce.payer.review.dto.ManualDecisionResponse;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.dto.ReviewSummary;
import com.lifeforce.payer.review.service.ReviewDecisionService;
import com.lifeforce.payer.review.service.ReviewQueryService;
import com.lifeforce.payer.review.service.ReviewEvidenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewQueryService reviewQueryService;
    private final ReviewDecisionService reviewDecisionService;
    private final ReviewEvidenceService reviewEvidenceService;

    public ReviewController(ReviewQueryService reviewQueryService, ReviewDecisionService reviewDecisionService, ReviewEvidenceService reviewEvidenceService) {
        this.reviewQueryService = reviewQueryService;
        this.reviewDecisionService = reviewDecisionService;
        this.reviewEvidenceService = reviewEvidenceService;
    }

    @GetMapping("")
    public ResponseEntity<List<ReviewSummary>> getReviews(@RequestParam(name = "status", defaultValue = "PENDING_MANUAL_REVIEW") ReviewStatus reviewStatus) {
        return ResponseEntity.ok(reviewQueryService.getReviews(reviewStatus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDetails> getReviewDetails(@PathVariable("id") UUID reviewId) {
        Optional<ReviewDetails> review = reviewQueryService.getReviewDetails(reviewId);
        if (review.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review does not exist");
        }
        return ResponseEntity.ok(review.get());
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<ManualDecisionResponse> submitManualDecision(@PathVariable("id") UUID reviewId, @RequestBody @Valid ManualDecisionRequest decisionRequest) {
        return ResponseEntity.ok(reviewDecisionService.submitManualDecision(reviewId, decisionRequest));
    }

    @PostMapping("/{id}/evidence-requests")
    public ResponseEntity<EvidenceRequestResponse> requestEvidence(@PathVariable("id") UUID reviewId, @RequestBody @Valid EvidenceRequest evidenceRequest) {
        return ResponseEntity.ok(reviewEvidenceService.requestEvidence(reviewId, evidenceRequest));
    }
}

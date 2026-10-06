package com.lifeforce.payer.review.controller;

import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.dto.ReviewSummary;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public ReviewController(ReviewQueryService reviewQueryService) {
        this.reviewQueryService = reviewQueryService;
    }

    @GetMapping("")
    public ResponseEntity<List<ReviewSummary>> getReviews(@RequestParam(name = "status", defaultValue = "PENDING_MANUAL_REVIEW") ReviewStatus status) {
        return ResponseEntity.ok(reviewQueryService.getReviews(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDetails> getReviewDetails(@PathVariable("id") UUID reviewId) {
        Optional<ReviewDetails> review = reviewQueryService.getReviewDetails(reviewId);
        if (review.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(review.get());
    }
}

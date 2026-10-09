package com.lifeforce.payer.review.controller;

import com.lifeforce.payer.assistant.service.ReviewerAssistantService;
import com.lifeforce.payer.review.dto.ReviewDetails;
import com.lifeforce.payer.review.service.ReviewQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewAssistantController {
    private final ReviewQueryService reviewQueryService;
    private final ReviewerAssistantService assistantService;

    public ReviewAssistantController(ReviewQueryService reviewQueryService, ReviewerAssistantService assistantService) {
        this.reviewQueryService = reviewQueryService;
        this.assistantService = assistantService;
    }

    @PostMapping("/{reviewId}/assistant-summary")
    public AssistantSummaryResponse summarize(@PathVariable("reviewId") UUID reviewId) {
        ReviewDetails details = reviewQueryService.getReviewDetails(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review does not exist"));

        var result = assistantService.summarize(details);
        return new AssistantSummaryResponse(reviewId, result.summary(), result.referencePassages());
    }

    public record AssistantSummaryResponse(UUID reviewId, String summary,
            List<ReviewerAssistantService.ReferencePassage> referencePassages) {}
}

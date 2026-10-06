package com.lifeforce.payer.review.scheduler;

import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ReviewScheduler.class);

    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    public ReviewScheduler(ReviewRepository reviewRepository, ReviewService reviewService) {
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void processPendingReviews() {
        List <Review> pendingReviews = reviewRepository.findByReviewStatusOrderByLastUpdatedAsc(ReviewStatus.PENDING_EVALUATION);

        for (Review review : pendingReviews) {
            try {
                reviewService.evaluateReview(review.getId());
            } catch (Exception exception) {
                logger.error("Failed to process review {}", review.getId(), exception);
            }
        }
    }

}

package com.lifeforce.payer.review;

import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import com.lifeforce.payer.review.repository.ReviewRepository;
import com.lifeforce.payer.review.scheduler.ReviewScheduler;
import com.lifeforce.payer.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewSchedulerTests {
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewService reviewService;

    @Test
    void evaluatesPendingReviewIdsInRepositoryOrder() {
        Review firstReview = Review.createNewReview(UUID.randomUUID(), Clock.systemUTC());
        Review secondReview = Review.createNewReview(UUID.randomUUID(), Clock.systemUTC());
        when(reviewRepository.findByReviewStatusOrderByLastUpdatedAsc(ReviewStatus.PENDING_EVALUATION)).thenReturn(List.of(firstReview, secondReview));
        ReviewScheduler scheduler = new ReviewScheduler(reviewRepository, reviewService);

        scheduler.processPendingReviews();

        var orderedCalls = inOrder(reviewService);
        orderedCalls.verify(reviewService).evaluateReview(firstReview.getId());
        orderedCalls.verify(reviewService).evaluateReview(secondReview.getId());
        verifyNoMoreInteractions(reviewService);
    }

    @Test
    void continuesEvaluatingWhenOneReviewFails() {
        Review firstReview = Review.createNewReview(UUID.randomUUID(), Clock.systemUTC());
        Review secondReview = Review.createNewReview(UUID.randomUUID(), Clock.systemUTC());
        when(reviewRepository.findByReviewStatusOrderByLastUpdatedAsc(ReviewStatus.PENDING_EVALUATION)).thenReturn(List.of(firstReview, secondReview));
        doThrow(new IllegalStateException("Review failed")).when(reviewService).evaluateReview(firstReview.getId());
        ReviewScheduler scheduler = new ReviewScheduler(reviewRepository, reviewService);

        scheduler.processPendingReviews();

        var orderedCalls = inOrder(reviewService);
        orderedCalls.verify(reviewService).evaluateReview(firstReview.getId());
        orderedCalls.verify(reviewService).evaluateReview(secondReview.getId());
        verifyNoMoreInteractions(reviewService);
    }

    @Test
    void doesNotCallServiceWhenNoPendingReviewsExist() {
        ReviewScheduler scheduler = new ReviewScheduler(reviewRepository, reviewService);

        scheduler.processPendingReviews();

        verify(reviewRepository).findByReviewStatusOrderByLastUpdatedAsc(ReviewStatus.PENDING_EVALUATION);
        verifyNoInteractions(reviewService);
    }
}

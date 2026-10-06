package com.lifeforce.payer.review.repository;

import com.lifeforce.payer.review.domain.ReviewHistory;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface ReviewHistoryRepository extends Repository<ReviewHistory, UUID> {
    ReviewHistory save(ReviewHistory history);

    List<ReviewHistory> findByReviewIdOrderByEventAtAscIdAsc(UUID reviewId);
}

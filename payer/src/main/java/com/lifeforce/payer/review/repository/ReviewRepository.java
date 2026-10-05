package com.lifeforce.payer.review.repository;

import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends Repository<Review, UUID> {
    Review save(Review review);

    List<Review> findByReviewStatusOrderByLastUpdatedAsc(ReviewStatus reviewStatus);

    Optional<Review> findById(UUID id);
}

package com.lifeforce.payer.review.repository;

import com.lifeforce.payer.review.domain.Review;
import com.lifeforce.payer.review.domain.ReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends Repository<Review, UUID> {
    Review save(Review review);

    List<Review> findByReviewStatusOrderByLastUpdatedAsc(ReviewStatus reviewStatus);

    @EntityGraph(attributePaths = "authorizationRequest")
    List<Review> findByReviewStatusOrderByLastUpdatedAscIdAsc(ReviewStatus reviewStatus);

    @EntityGraph(attributePaths = "authorizationRequest")
    Optional<Review> findById(UUID id);
}

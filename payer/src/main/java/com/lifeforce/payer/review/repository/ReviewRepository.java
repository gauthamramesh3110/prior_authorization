package com.lifeforce.payer.review.repository;

import com.lifeforce.payer.review.domain.Review;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface ReviewRepository extends Repository<Review, UUID> {
    Review save(Review review);
}

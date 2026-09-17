package com.iloveshopping.repository;

import com.iloveshopping.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, String> {

    Optional<ReviewVote> findByReviewIdAndUserId(String reviewId, String userId);

    boolean existsByReviewIdAndUserId(String reviewId, String userId);

    long countByReviewId(String reviewId);
}

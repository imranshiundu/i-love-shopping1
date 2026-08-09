package com.iloveshopping.repository;

import com.iloveshopping.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    Optional<Review> findByProductIdAndUserId(String productId, String userId);

    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double getAverageRating(@Param("productId") String productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId")
    Long getReviewCount(@Param("productId") String productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.rating = :rating")
    Long getRatingCount(@Param("productId") String productId, @Param("rating") Integer rating);

    boolean existsByProductIdAndUserId(String productId, String userId);
}
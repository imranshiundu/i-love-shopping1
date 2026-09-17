package com.iloveshopping.repository;

import com.iloveshopping.entity.Review;
import com.iloveshopping.entity.ReviewVote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED' ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    Page<Review> findApprovedByHelpfulness(@Param("productId") String productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED' ORDER BY r.createdAt DESC")
    Page<Review> findApprovedByRecency(@Param("productId") String productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    Page<Review> findAllByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    Double getAverageRating(@Param("productId") String productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    Long getReviewCount(@Param("productId") String productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.rating = :rating AND r.status = 'APPROVED'")
    Long getRatingCount(@Param("productId") String productId, @Param("rating") Integer rating);

    boolean existsByProductIdAndUserId(String productId, String userId);
}

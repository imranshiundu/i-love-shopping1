package com.iloveshopping.service;

import com.iloveshopping.dto.user.ReviewRequest;
import com.iloveshopping.dto.user.ReviewResponse;
import com.iloveshopping.entity.Product;
import com.iloveshopping.entity.Review;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.ProductRepository;
import com.iloveshopping.repository.ReviewRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Page<ReviewResponse> getProductReviews(String productSlug, int page, int size) {
        Product product = productRepository.findBySlug(productSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", productSlug));

        PageRequest pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId(), pageable);
        return reviewPage.map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse addReview(String productSlug, ReviewRequest request) {
        User user = getCurrentUser();
        Product product = productRepository.findBySlug(productSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", productSlug));

        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .isVerifiedPurchase(isVerifiedPurchase(user, product))
                .build();

        review = reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse updateReview(String id, ReviewRequest request) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review", "id", id);
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());

        review = reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(String id) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review", "id", id);
        }

        reviewRepository.deleteById(id);
    }

    private boolean isVerifiedPurchase(User user, Product product) {
        return false;
    }

    private User getCurrentUser() {
        return null;
    }
}
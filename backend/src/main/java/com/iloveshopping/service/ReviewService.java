package com.iloveshopping.service;

import com.iloveshopping.dto.user.ReviewRequest;
import com.iloveshopping.dto.user.ReviewResponse;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.OrderItem;
import com.iloveshopping.entity.Product;
import com.iloveshopping.entity.Review;
import com.iloveshopping.entity.ReviewVote;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.AuthenticationException;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.ProductRepository;
import com.iloveshopping.repository.ReviewRepository;
import com.iloveshopping.repository.ReviewVoteRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReviewService {

    private static final Set<Order.OrderStatus> PURCHASE_STATUSES = Set.of(
            Order.OrderStatus.CONFIRMED, Order.OrderStatus.PROCESSING,
            Order.OrderStatus.SHIPPED, Order.OrderStatus.DELIVERED,
            Order.OrderStatus.REFUNDED);

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public Page<ReviewResponse> getProductReviews(String productSlug, int page, int size, String sortBy) {
        Product product = productRepository.findBySlug(productSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", productSlug));

        PageRequest pageable = PageRequest.of(page, size);
        boolean byHelpful = "helpful".equalsIgnoreCase(sortBy);
        Page<Review> reviewPage = byHelpful
                ? reviewRepository.findApprovedByHelpfulness(product.getId(), pageable)
                : reviewRepository.findApprovedByRecency(product.getId(), pageable);

        String viewerId = currentUserId();
        return reviewPage.map(review -> ReviewResponse.from(review, hasVoted(review.getId(), viewerId)));
    }

    @Transactional
    public ReviewResponse addReview(String productSlug, ReviewRequest request) {
        User user = requireCurrentUser();
        Product product = productRepository.findBySlug(productSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", productSlug));

        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw new IllegalStateException("You have already reviewed this product");
        }

        boolean verified = isVerifiedPurchase(user, product);

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .isVerifiedPurchase(verified)
                // Verified purchases publish immediately; everything else
                // waits for admin moderation.
                .status(verified ? Review.Status.APPROVED : Review.Status.PENDING)
                .build();

        review = reviewRepository.save(review);
        log.info("Review added for product {} by {} (verified={}, status={})",
                product.getSlug(), user.getEmail(), verified, review.getStatus());
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse updateReview(String id, ReviewRequest request) {
        User user = requireCurrentUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review", "id", id);
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        // Edited content re-enters moderation unless it was a verified purchase.
        if (review.getStatus() == Review.Status.REJECTED
                || (review.getStatus() == Review.Status.APPROVED && !Boolean.TRUE.equals(review.getIsVerifiedPurchase()))) {
            review.setStatus(Review.Status.PENDING);
        }

        review = reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(String id) {
        User user = requireCurrentUser();
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Review", "id", id);
        }

        reviewRepository.deleteById(id);
    }

    /**
     * Toggles the current user's "helpful" vote on a review and keeps the
     * denormalized helpfulCount in sync.
     */
    @Transactional
    public ReviewResponse toggleHelpful(String reviewId) {
        User user = requireCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        var existing = reviewVoteRepository.findByReviewIdAndUserId(reviewId, user.getId());
        boolean voted;
        if (existing.isPresent()) {
            reviewVoteRepository.delete(existing.get());
            review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
            voted = false;
        } else {
            reviewVoteRepository.save(ReviewVote.builder().review(review).user(user).build());
            review.setHelpfulCount(review.getHelpfulCount() + 1);
            voted = true;
        }

        review = reviewRepository.save(review);
        return ReviewResponse.from(review, voted);
    }

    // ---- Admin moderation ----

    public Page<ReviewResponse> getAllReviews(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        String normalized = (status == null || status.isBlank() || "all".equalsIgnoreCase(status))
                ? null : status.toUpperCase();
        Page<Review> reviewPage;
        if (normalized == null) {
            reviewPage = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            Review.Status filter;
            try {
                filter = Review.Status.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: must be PENDING, APPROVED or REJECTED");
            }
            reviewPage = reviewRepository.findByStatus(filter, pageable);
        }
        return reviewPage.map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse moderateReview(String id, String status) {
        Review.Status newStatus;
        try {
            newStatus = Review.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status must be PENDING, APPROVED or REJECTED");
        }
        if (newStatus == Review.Status.PENDING) {
            throw new IllegalArgumentException("Reviews can only be approved or rejected");
        }

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        review.setStatus(newStatus);
        review = reviewRepository.save(review);
        log.info("Review {} moderated to {}", id, newStatus);
        return ReviewResponse.from(review);
    }

    /**
     * A purchase counts as verified when the reviewer has an order containing
     * the product that progressed past the pending stage (confirmed,
     * processing, shipped, delivered or refunded).
     */
    private boolean isVerifiedPurchase(User user, Product product) {
        List<Order> orders = orderRepository.findByUserIdAndStatusIn(user.getId(),
                List.copyOf(PURCHASE_STATUSES));
        for (Order order : orders) {
            if (!PURCHASE_STATUSES.contains(order.getStatus())) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null && item.getProduct().getId().equals(product.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasVoted(String reviewId, String viewerId) {
        return viewerId != null && reviewVoteRepository.existsByReviewIdAndUserId(reviewId, viewerId);
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return userRepository.findById(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
        }
        return null;
    }

    /**
     * Review endpoints live under permitAll paths (/products/**), so an
     * unauthenticated caller reaches this service. Fail with a proper 401
     * instead of an NPE.
     */
    private User requireCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            throw AuthenticationException.invalidToken();
        }
        return user;
    }
}

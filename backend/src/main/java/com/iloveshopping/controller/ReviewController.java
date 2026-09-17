package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.user.ReviewListResponse;
import com.iloveshopping.dto.user.ReviewRequest;
import com.iloveshopping.dto.user.ReviewResponse;
import com.iloveshopping.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews, ratings and moderation")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{slug}/reviews")
    @Operation(summary = "Get approved product reviews (sortBy: helpful|newest)")
    public ResponseEntity<ApiResponse<ReviewListResponse>> getProductReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "helpful") String sortBy) {

        Page<ReviewResponse> reviews = reviewService.getProductReviews(slug, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(toListResponse(reviews)));
    }

    @PostMapping("/products/{slug}/reviews")
    @Operation(summary = "Add a review for a product")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable String slug,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse review = reviewService.addReview(slug, request);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @PutMapping("/reviews/{id}")
    @Operation(summary = "Update a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse review = reviewService.updateReview(id, request);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @DeleteMapping("/reviews/{id}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String id) {

        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reviews/{id}/helpful")
    @Operation(summary = "Toggle the current user's helpful vote on a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> toggleHelpful(
            @PathVariable String id) {

        ReviewResponse review = reviewService.toggleHelpful(id);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    // ---- Admin moderation ----

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all reviews for moderation (admin)")
    public ResponseEntity<ApiResponse<ReviewListResponse>> getAllReviews(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReviewResponse> reviews = reviewService.getAllReviews(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(toListResponse(reviews)));
    }

    @PutMapping("/admin/reviews/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve or reject a review (admin)")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable String id,
            @RequestParam String status) {

        ReviewResponse review = reviewService.moderateReview(id, status);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    private ReviewListResponse toListResponse(Page<ReviewResponse> reviews) {
        return ReviewListResponse.builder()
                .reviews(reviews.getContent())
                .pagination(ReviewListResponse.PageInfo.builder()
                        .page(reviews.getNumber())
                        .size(reviews.getSize())
                        .totalElements(reviews.getTotalElements())
                        .totalPages(reviews.getTotalPages())
                        .build())
                .build();
    }
}

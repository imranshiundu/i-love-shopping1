package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.user.ReviewRequest;
import com.iloveshopping.dto.user.ReviewResponse;
import com.iloveshopping.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{slug}/reviews")
    @Operation(summary = "Get product reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ReviewResponse> reviews = reviewService.getProductReviews(slug, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
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
}
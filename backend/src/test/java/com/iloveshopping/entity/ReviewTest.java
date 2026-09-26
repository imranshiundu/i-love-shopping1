package com.iloveshopping.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Review moderation states + rating validation + response mapping.
 */
class ReviewTest {

    @Test
    void newReviewsDefaultToPending() {
        Review review = Review.builder().rating(4).build();
        assertEquals(Review.Status.PENDING, review.getStatus(), "fresh reviews must await moderation");
        assertEquals(0, review.getHelpfulCount());
    }

    @Test
    void ratingMustBeBetweenOneAndFive() {
        Review review = Review.builder().rating(0).build();
        assertThrows(IllegalArgumentException.class, review::validateRating);
        review.setRating(6);
        assertThrows(IllegalArgumentException.class, review::validateRating);
        review.setRating(3);
        assertDoesNotThrow(review::validateRating);
    }

    @Test
    void moderationTransitions() {
        Review review = Review.builder().rating(5).build();
        assertEquals(Review.Status.PENDING, review.getStatus());
        review.setStatus(Review.Status.APPROVED);
        assertEquals(Review.Status.APPROVED, review.getStatus());
        review.setStatus(Review.Status.REJECTED);
        assertEquals(Review.Status.REJECTED, review.getStatus());
    }
}

package com.agrochain.backend.controller;

import com.agrochain.backend.dto.AverageRatingResponse;
import com.agrochain.backend.dto.ReviewResponse;
import com.agrochain.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public — reviews and rating summaries are safe to show unauthenticated,
// same visibility rule as GET /users/{id} and /users/{id}/followers.
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getUserReviews(id));
    }

    @GetMapping("/{id}/average-rating")
    public ResponseEntity<AverageRatingResponse> getAverageRating(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getAverageRating(id));
    }
}

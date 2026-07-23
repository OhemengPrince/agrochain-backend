package com.agrochain.backend.service;

import com.agrochain.backend.dto.AverageRatingResponse;
import com.agrochain.backend.dto.ReviewResponse;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.model.Review;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.ReviewRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public List<ReviewResponse> getUserReviews(Long userId) {
        User user = findUserOrThrow(userId);

        return reviewRepository.findByRevieweeOrderByCreatedAtDesc(user).stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .reviewerName(review.getReviewer().getFullName())
                        .reviewerAvatar(review.getReviewer().getProfilePhotoUrl())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build())
                .toList();
    }

    public AverageRatingResponse getAverageRating(Long userId) {
        User user = findUserOrThrow(userId);

        List<Review> reviews = reviewRepository.findByReviewee(user);
        Double averageRating = reviews.isEmpty() ? null
                : Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0;

        return AverageRatingResponse.builder()
                .averageRating(averageRating)
                .totalReviews(reviews.size())
                .build();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

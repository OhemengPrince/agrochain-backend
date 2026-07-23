package com.agrochain.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Body for the path-based POST /bookings/{id}/review — bookingId comes from
// the path, so unlike ReviewRequest this carries only rating/comment.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private String comment;
}

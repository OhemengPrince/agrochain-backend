package com.agrochain.backend.controller;

import com.agrochain.backend.dto.BookingResponse;
import com.agrochain.backend.dto.BookingReviewRequest;
import com.agrochain.backend.dto.CreateBookingRequest;
import com.agrochain.backend.dto.ReviewRequest;
import com.agrochain.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(Authentication authentication,
                                                          @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(authentication.getName(), request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<BookingResponse>> getIncomingBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getIncomingBookings(authentication.getName()));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirmBooking(authentication.getName(), id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(authentication.getName(), id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> completeBooking(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.completeBooking(authentication.getName(), id));
    }

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, String>> submitReview(Authentication authentication,
                                                              @Valid @RequestBody ReviewRequest request) {
        bookingService.submitReview(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Review submitted successfully."));
    }

    // Path-based equivalent of POST /bookings/reviews (which takes bookingId
    // in the body) — same bookingService.submitReview underneath, so the same
    // validation applies: booking must exist, be COMPLETED, belong to the
    // caller, and not already be reviewed.
    @PostMapping("/{id}/review")
    public ResponseEntity<Map<String, String>> reviewBooking(Authentication authentication, @PathVariable Long id,
                                                               @Valid @RequestBody BookingReviewRequest request) {
        ReviewRequest reviewRequest = ReviewRequest.builder()
                .bookingId(id)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        bookingService.submitReview(authentication.getName(), reviewRequest);
        return ResponseEntity.ok(Map.of("message", "Review submitted successfully."));
    }
}

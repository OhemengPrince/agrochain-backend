package com.agrochain.backend.repository;

import com.agrochain.backend.model.Booking;
import com.agrochain.backend.model.Review;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBooking(Booking booking);

    List<Review> findByReviewee(User reviewee);

    List<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee);
}

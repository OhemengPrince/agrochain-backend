package com.agrochain.backend.repository;

import com.agrochain.backend.model.Booking;
import com.agrochain.backend.model.Payment;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUser(User user);

    Optional<Payment> findByPaystackReference(String paystackReference);

    List<Payment> findByBooking(Booking booking);
}

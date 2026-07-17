package com.agrochain.backend.service;

import com.agrochain.backend.dto.BookingResponse;
import com.agrochain.backend.dto.CreateBookingRequest;
import com.agrochain.backend.dto.ReviewRequest;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.*;
import com.agrochain.backend.repository.BookingRepository;
import com.agrochain.backend.repository.EquipmentRepository;
import com.agrochain.backend.repository.PaymentRepository;
import com.agrochain.backend.repository.ReviewRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final FollowService followService;
    private final PaymentRepository paymentRepository;
    private final EarningsService earningsService;

    private static final BigDecimal BUYER_FEE_RATE = new BigDecimal("0.05");

    public BookingResponse createBooking(String farmerEmail, CreateBookingRequest request) {
        User farmer = getUserOrThrow(farmerEmail);
        if (farmer.getRole() != Role.FARMER) {
            throw new UnauthorizedException("Only farmers can book equipment");
        }

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        if (!equipment.isAvailable()) {
            throw new BadRequestException("Equipment is not available for booking");
        }

        boolean hasOverlap = bookingRepository
                .findByEquipmentAndStatusIn(equipment, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .stream()
                .anyMatch(existing -> !request.getStartDate().isAfter(existing.getEndDate())
                        && !existing.getStartDate().isAfter(request.getEndDate()));
        if (hasOverlap) {
            throw new BadRequestException("Equipment is already booked for the selected dates");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        BigDecimal totalCost = equipment.getDailyRate().multiply(BigDecimal.valueOf(days));

        Booking booking = Booking.builder()
                .equipment(equipment)
                .farmer(farmer)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalCost(totalCost)
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        Booking saved = bookingRepository.save(booking);

        notificationService.createNotification(
                equipment.getOwner().getId(),
                "New Booking Request",
                farmer.getFullName() + " requested to book your " + equipment.getName()
                        + " from " + request.getStartDate() + " to " + request.getEndDate(),
                NotificationType.BOOKING);

        return BookingMapper.toResponse(saved);
    }

    public List<BookingResponse> getMyBookings(String farmerEmail) {
        User farmer = getUserOrThrow(farmerEmail);
        return bookingRepository.findByFarmer(farmer).stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    public List<BookingResponse> getIncomingBookings(String ownerEmail) {
        User owner = getUserOrThrow(ownerEmail);
        return bookingRepository.findByEquipment_Owner(owner).stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    public BookingResponse confirmBooking(String ownerEmail, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getEquipment().getOwner().getEmail().equals(ownerEmail)) {
            throw new UnauthorizedException("You do not own the equipment for this booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        notificationService.createNotification(
                booking.getFarmer().getId(),
                "Booking Confirmed",
                "Your booking for " + booking.getEquipment().getName() + " has been confirmed.",
                NotificationType.BOOKING);

        User owner = booking.getEquipment().getOwner();
        followService.notifyFollowers(owner.getId(),
                owner.getFullName() + " accepted a booking for " + booking.getEquipment().getName(),
                NotificationType.BOOKING_ACCEPTED);

        return BookingMapper.toResponse(saved);
    }

    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        boolean isFarmer = booking.getFarmer().getEmail().equals(userEmail);
        boolean isOwner = booking.getEquipment().getOwner().getEmail().equals(userEmail);
        if (!isFarmer && !isOwner) {
            throw new UnauthorizedException("You are not part of this booking");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This booking can no longer be cancelled");
        }

        boolean wasPaid = booking.getPaymentStatus() == PaymentStatus.PAID;
        booking.setStatus(BookingStatus.CANCELLED);
        if (wasPaid) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        Booking saved = bookingRepository.save(booking);

        // Refund is ledger-only here (a Transaction row recording what's owed
        // back to the buyer) — it does not call Paystack's refund API to
        // actually move money back to their mobile money account. That's a
        // separate integration this task didn't ask for.
        if (wasPaid) {
            findSuccessfulPayment(booking).ifPresent(payment -> {
                String reference = payment.getPaystackReference();
                earningsService.reverseEarning(reference + "-INCOME");

                BigDecimal baseAmount = booking.getTotalCost();
                BigDecimal buyerFee = baseAmount.multiply(BUYER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal buyerTotal = baseAmount.add(buyerFee);
                BigDecimal cancellationFee = buyerTotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal refundAmount = buyerTotal.subtract(cancellationFee);

                earningsService.recordTransaction(booking.getFarmer().getId(), TransactionType.REFUND,
                        buyerTotal, cancellationFee, refundAmount, EarningsTransactionStatus.COMPLETED,
                        "Refund for cancelled booking of " + booking.getEquipment().getName(),
                        reference + "-REFUND", booking.getEquipment().getOwner().getFullName(),
                        booking.getId(), null, null, null);
            });
        }

        notificationService.createNotification(
                booking.getFarmer().getId(),
                "Booking Cancelled",
                "The booking for " + booking.getEquipment().getName() + " has been cancelled.",
                NotificationType.BOOKING);
        notificationService.createNotification(
                booking.getEquipment().getOwner().getId(),
                "Booking Cancelled",
                "The booking for " + booking.getEquipment().getName() + " has been cancelled.",
                NotificationType.BOOKING);

        return BookingMapper.toResponse(saved);
    }

    // This is where the equipment owner's payout actually releases from
    // pending_balance to available_balance — not confirmBooking(), which only
    // means the owner accepted the request before the rental period even
    // happened. Releasing funds there would pay the owner before delivering
    // anything; completeBooking() only fires after the rental is done.
    public BookingResponse completeBooking(String ownerEmail, Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getEquipment().getOwner().getEmail().equals(ownerEmail)) {
            throw new UnauthorizedException("You do not own the equipment for this booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        findSuccessfulPayment(booking).ifPresent(payment ->
                earningsService.confirmEarning(payment.getPaystackReference() + "-INCOME"));

        notificationService.createNotification(
                booking.getFarmer().getId(),
                "Booking Completed",
                "Your booking for " + booking.getEquipment().getName() + " has been marked as completed.",
                NotificationType.BOOKING);

        return BookingMapper.toResponse(saved);
    }

    private Optional<Payment> findSuccessfulPayment(Booking booking) {
        return paymentRepository.findByBooking(booking).stream()
                .filter(p -> p.getStatus() == TransactionStatus.SUCCESS)
                .findFirst();
    }

    public void submitReview(String userEmail, ReviewRequest request) {
        Booking booking = findBookingOrThrow(request.getBookingId());

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be reviewed");
        }

        User reviewer = getUserOrThrow(userEmail);
        boolean isFarmer = booking.getFarmer().getEmail().equals(userEmail);
        boolean isOwner = booking.getEquipment().getOwner().getEmail().equals(userEmail);
        if (!isFarmer && !isOwner) {
            throw new UnauthorizedException("You are not part of this booking");
        }

        if (reviewRepository.findByBooking(booking).isPresent()) {
            throw new BadRequestException("This booking has already been reviewed");
        }

        User reviewee = isFarmer ? booking.getEquipment().getOwner() : booking.getFarmer();

        Review review = Review.builder()
                .booking(booking)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);
    }

    private Booking findBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

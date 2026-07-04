package com.agrochain.backend.service;

import com.agrochain.backend.dto.BookingResponse;
import com.agrochain.backend.model.Booking;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .equipmentId(booking.getEquipment().getId())
                .equipmentName(booking.getEquipment().getName())
                .farmerName(booking.getFarmer().getFullName())
                .ownerName(booking.getEquipment().getOwner().getFullName())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}

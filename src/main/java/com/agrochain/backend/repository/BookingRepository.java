package com.agrochain.backend.repository;

import com.agrochain.backend.model.Booking;
import com.agrochain.backend.model.BookingStatus;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByFarmer(User farmer);

    List<Booking> findByEquipment_Owner(User owner);

    List<Booking> findByStatus(BookingStatus status);
}

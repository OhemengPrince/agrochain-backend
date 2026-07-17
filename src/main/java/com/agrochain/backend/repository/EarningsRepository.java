package com.agrochain.backend.repository;

import com.agrochain.backend.model.Earnings;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EarningsRepository extends JpaRepository<Earnings, Long> {

    Optional<Earnings> findByUser(User user);

    Optional<Earnings> findByUserId(Long userId);
}

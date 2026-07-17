package com.agrochain.backend.repository;

import com.agrochain.backend.model.User;
import com.agrochain.backend.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByUserOrderByCreatedAtDesc(User user);
}

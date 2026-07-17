package com.agrochain.backend.repository;

import com.agrochain.backend.model.Transaction;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<Transaction> findByReference(String reference);
}

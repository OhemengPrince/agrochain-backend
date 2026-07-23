package com.agrochain.backend.repository;

import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.Transaction;
import com.agrochain.backend.model.TransactionType;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Transaction> findByUser(User user);

    Optional<Transaction> findByReference(String reference);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> search(@Param("user") User user,
                              @Param("type") TransactionType type,
                              @Param("status") EarningsTransactionStatus status,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> searchAll(@Param("user") User user,
                                 @Param("type") TransactionType type,
                                 @Param("status") EarningsTransactionStatus status,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}

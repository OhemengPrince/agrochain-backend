package com.agrochain.backend.repository;

import com.agrochain.backend.model.ProducePurchase;
import com.agrochain.backend.model.PurchaseStatus;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProducePurchaseRepository extends JpaRepository<ProducePurchase, Long> {

    Page<ProducePurchase> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);

    @Query("SELECT p FROM ProducePurchase p WHERE p.batch.farmer = :farmer ORDER BY p.createdAt DESC")
    Page<ProducePurchase> findByBatchFarmer(@Param("farmer") User farmer, Pageable pageable);

    Optional<ProducePurchase> findByPaystackReference(String paystackReference);

    List<ProducePurchase> findByStatusAndAutoConfirmAtBefore(PurchaseStatus status, LocalDateTime before);
}

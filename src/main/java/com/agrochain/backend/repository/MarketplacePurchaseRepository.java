package com.agrochain.backend.repository;

import com.agrochain.backend.model.MarketplacePurchase;
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

public interface MarketplacePurchaseRepository extends JpaRepository<MarketplacePurchase, Long> {

    Page<MarketplacePurchase> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);

    @Query("SELECT p FROM MarketplacePurchase p WHERE p.listing.seller = :seller ORDER BY p.createdAt DESC")
    Page<MarketplacePurchase> findByListingSeller(@Param("seller") User seller, Pageable pageable);

    Optional<MarketplacePurchase> findByPaystackReference(String paystackReference);

    List<MarketplacePurchase> findByStatusAndAutoConfirmAtBefore(PurchaseStatus status, LocalDateTime before);
}

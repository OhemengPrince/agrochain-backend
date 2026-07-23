package com.agrochain.backend.repository;

import com.agrochain.backend.model.ListingCategory;
import com.agrochain.backend.model.ListingStatus;
import com.agrochain.backend.model.MarketplaceListing;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long> {

    List<MarketplaceListing> findBySeller(User seller);

    List<MarketplaceListing> findByCategory(ListingCategory category);

    List<MarketplaceListing> findByStatus(ListingStatus status);

    @Query("SELECT l FROM MarketplaceListing l WHERE " +
            "l.status = com.agrochain.backend.model.ListingStatus.ACTIVE AND " +
            "(:category IS NULL OR l.category = :category) AND " +
            "(:region IS NULL OR l.region = :region) AND " +
            "(:query IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) AND " +
            "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR l.price <= :maxPrice) " +
            "ORDER BY l.createdAt DESC")
    List<MarketplaceListing> search(@Param("category") ListingCategory category,
                                     @Param("region") String region,
                                     @Param("query") String query,
                                     @Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT l FROM MarketplaceListing l WHERE " +
            "l.status = com.agrochain.backend.model.ListingStatus.ACTIVE AND " +
            "(LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
            "LOWER(CAST(l.category AS string)) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))")
    List<MarketplaceListing> searchActive(@Param("query") String query, Pageable pageable);
}

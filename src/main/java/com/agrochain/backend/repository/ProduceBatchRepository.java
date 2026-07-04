package com.agrochain.backend.repository;

import com.agrochain.backend.model.BatchStatus;
import com.agrochain.backend.model.ProduceBatch;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProduceBatchRepository extends JpaRepository<ProduceBatch, Long> {

    List<ProduceBatch> findByFarmer(User farmer);

    List<ProduceBatch> findByStatus(BatchStatus status);

    Optional<ProduceBatch> findByQrCodeValue(String qrCodeValue);

    @Query("SELECT b FROM ProduceBatch b WHERE " +
            "b.status = com.agrochain.backend.model.BatchStatus.READY_FOR_SALE AND " +
            "(:region IS NULL OR b.region = :region) AND " +
            "(:district IS NULL OR b.district = :district) AND " +
            "(:query IS NULL OR LOWER(b.cropName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))")
    Page<ProduceBatch> findCatalogue(@Param("region") String region,
                                      @Param("district") String district,
                                      @Param("query") String query,
                                      Pageable pageable);
}

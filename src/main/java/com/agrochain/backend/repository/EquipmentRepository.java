package com.agrochain.backend.repository;

import com.agrochain.backend.model.Equipment;
import com.agrochain.backend.model.EquipmentCategory;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByOwner(User owner);

    List<Equipment> findByRegion(String region);

    List<Equipment> findByCategory(EquipmentCategory category);

    @Query("SELECT e FROM Equipment e WHERE " +
            "(:region IS NULL OR e.region = :region) AND " +
            "(:district IS NULL OR e.district = :district) AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:query IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) AND " +
            "e.isAvailable = true")
    Page<Equipment> search(@Param("region") String region,
                            @Param("district") String district,
                            @Param("category") EquipmentCategory category,
                            @Param("query") String query,
                            Pageable pageable);
}

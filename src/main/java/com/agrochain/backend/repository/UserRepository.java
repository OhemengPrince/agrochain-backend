package com.agrochain.backend.repository;

import com.agrochain.backend.model.Role;
import com.agrochain.backend.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findByIsVerifiedTrueOrderByCreatedAtDesc(Pageable pageable);

    // Only reviewees with at least one review appear at all, since the query
    // is driven by an inner join against Review rather than a left join from User.
    @Query("SELECT u, AVG(r.rating), COUNT(r) FROM User u JOIN Review r ON r.reviewee = u " +
            "WHERE u.isVerified = true " +
            "AND (:role IS NULL OR u.role = :role) " +
            "GROUP BY u " +
            "ORDER BY AVG(r.rating) DESC")
    List<Object[]> findTopRatedUsers(@Param("role") Role role, Pageable pageable);

    // Left join so verified users with zero reviews still appear in search
    // results (AVG(r.rating) comes back null for them).
    @Query("SELECT u, AVG(r.rating) FROM User u LEFT JOIN Review r ON r.reviewee = u " +
            "WHERE u.isVerified = true " +
            "AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
            "GROUP BY u")
    List<Object[]> searchVerified(@Param("query") String query, Pageable pageable);
}

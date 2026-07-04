package com.agrochain.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "produce_batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduceBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @Column(nullable = false)
    private String cropName;

    private String variety;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String district;

    private LocalDate plantedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(unique = true)
    private String qrCodeValue;

    @Column(length = 4000)
    private String inputs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

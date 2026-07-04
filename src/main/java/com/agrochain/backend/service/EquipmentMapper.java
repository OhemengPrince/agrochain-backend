package com.agrochain.backend.service;

import com.agrochain.backend.dto.EquipmentResponse;
import com.agrochain.backend.model.Equipment;

public final class EquipmentMapper {

    private EquipmentMapper() {
    }

    public static EquipmentResponse toResponse(Equipment equipment) {
        return EquipmentResponse.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .description(equipment.getDescription())
                .dailyRate(equipment.getDailyRate())
                .region(equipment.getRegion())
                .district(equipment.getDistrict())
                .imageUrl(equipment.getImageUrl())
                .isAvailable(equipment.isAvailable())
                .ownerName(equipment.getOwner().getFullName())
                .ownerPhone(equipment.getOwner().getPhoneNumber())
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }
}

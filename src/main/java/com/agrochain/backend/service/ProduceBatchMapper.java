package com.agrochain.backend.service;

import com.agrochain.backend.dto.BatchResponse;
import com.agrochain.backend.dto.BatchStageResponse;
import com.agrochain.backend.model.BatchStage;
import com.agrochain.backend.model.ProduceBatch;

import java.util.List;

public final class ProduceBatchMapper {

    private ProduceBatchMapper() {
    }

    public static BatchStageResponse toStageResponse(BatchStage stage) {
        return BatchStageResponse.builder()
                .id(stage.getId())
                .stageName(stage.getStageName())
                .description(stage.getDescription())
                .location(stage.getLocation())
                .createdAt(stage.getCreatedAt())
                .build();
    }

    public static BatchResponse toResponse(ProduceBatch batch, List<BatchStage> stages) {
        return BatchResponse.builder()
                .id(batch.getId())
                .cropName(batch.getCropName())
                .variety(batch.getVariety())
                .quantityKg(batch.getQuantityKg())
                .pricePerKg(batch.getPricePerKg())
                .region(batch.getRegion())
                .district(batch.getDistrict())
                .plantedDate(batch.getPlantedDate())
                .status(batch.getStatus())
                .qrCodeValue(batch.getQrCodeValue())
                .inputs(batch.getInputs())
                .farmerId(batch.getFarmer().getId())
                .farmerName(batch.getFarmer().getFullName())
                .farmerPhone(batch.getFarmer().getPhoneNumber())
                .stages(stages.stream().map(ProduceBatchMapper::toStageResponse).toList())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}

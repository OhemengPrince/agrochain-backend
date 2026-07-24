package com.agrochain.backend.service;

import com.agrochain.backend.dto.BatchResponse;
import com.agrochain.backend.dto.BatchStageResponse;
import com.agrochain.backend.model.BatchStage;
import com.agrochain.backend.model.ProduceBatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public final class ProduceBatchMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, String>>> INPUTS_TYPE = new TypeReference<>() {
    };

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
                .inputs(parseInputs(batch.getInputs()))
                .farmerId(batch.getFarmer().getId())
                .farmerName(batch.getFarmer().getFullName())
                .farmerPhone(batch.getFarmer().getPhoneNumber())
                .stages(stages.stream().map(ProduceBatchMapper::toStageResponse).toList())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    // Tolerates rows created before inputs became structured JSON (null,
    // blank, or plain-text legacy values) by returning null rather than
    // failing the whole batch listing over one bad row.
    private static List<Map<String, String>> parseInputs(String inputs) {
        if (inputs == null || inputs.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(inputs, INPUTS_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Could not parse produce batch inputs as JSON, returning null: {}", e.getMessage());
            return null;
        }
    }
}

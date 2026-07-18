package com.agrochain.backend.service;

import com.agrochain.backend.dto.AddStageRequest;
import com.agrochain.backend.dto.BatchResponse;
import com.agrochain.backend.dto.CreateBatchRequest;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.*;
import com.agrochain.backend.repository.BatchStageRepository;
import com.agrochain.backend.repository.ProduceBatchRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduceBatchService {

    private final ProduceBatchRepository produceBatchRepository;
    private final BatchStageRepository batchStageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FollowService followService;

    public BatchResponse createBatch(String farmerEmail, CreateBatchRequest request) {
        User farmer = getUserOrThrow(farmerEmail);
        if (farmer.getRole() != Role.FARMER) {
            throw new UnauthorizedException("Only farmers can create produce batches");
        }

        ProduceBatch batch = ProduceBatch.builder()
                .farmer(farmer)
                .cropName(request.getCropName())
                .variety(request.getVariety())
                .quantityKg(request.getQuantityKg())
                .pricePerKg(request.getPricePerKg())
                .region(request.getRegion())
                .district(request.getDistrict())
                .plantedDate(request.getPlantedDate())
                .status(BatchStatus.GROWING)
                .inputs(request.getInputs())
                .build();

        ProduceBatch saved = produceBatchRepository.save(batch);
        saved.setQrCodeValue("AGROCHAIN-BATCH-" + saved.getId());
        saved = produceBatchRepository.save(saved);

        followService.notifyFollowers(farmer.getId(),
                farmer.getFullName() + " harvested " + saved.getQuantityKg() + "kg of " + saved.getCropName()
                        + " - Ready for sale",
                NotificationType.NEW_PRODUCE);

        return ProduceBatchMapper.toResponse(saved, List.of());
    }

    public List<BatchResponse> getMyBatches(String farmerEmail) {
        User farmer = getUserOrThrow(farmerEmail);
        return produceBatchRepository.findByFarmer(farmer).stream()
                .map(batch -> ProduceBatchMapper.toResponse(batch, getStages(batch)))
                .toList();
    }

    public BatchResponse getBatchById(Long id) {
        ProduceBatch batch = findBatchOrThrow(id);
        return ProduceBatchMapper.toResponse(batch, getStages(batch));
    }

    public BatchResponse addStage(String farmerEmail, Long batchId, AddStageRequest request) {
        ProduceBatch batch = findBatchOrThrow(batchId);
        requireOwnership(batch, farmerEmail);

        BatchStage stage = BatchStage.builder()
                .batch(batch)
                .stageName(request.getStageName())
                .description(request.getDescription())
                .location(request.getLocation())
                .build();
        batchStageRepository.save(stage);

        return ProduceBatchMapper.toResponse(batch, getStages(batch));
    }

    public BatchResponse updateStatus(String farmerEmail, Long batchId, BatchStatus status) {
        ProduceBatch batch = findBatchOrThrow(batchId);
        requireOwnership(batch, farmerEmail);

        batch.setStatus(status);
        ProduceBatch saved = produceBatchRepository.save(batch);

        notificationService.createNotification(
                batch.getFarmer().getId(),
                "Batch Status Updated",
                "Your produce batch \"" + batch.getCropName() + "\" is now " + status.name() + ".",
                NotificationType.BATCH);

        return ProduceBatchMapper.toResponse(saved, getStages(saved));
    }

    public List<BatchResponse> getCatalogue(String region, String district, String query, int size) {
        Pageable pageable = PageRequest.of(0, size);
        return produceBatchRepository.findCatalogue(region, district, query, pageable).stream()
                .map(batch -> ProduceBatchMapper.toResponse(batch, getStages(batch)))
                .toList();
    }

    public BatchResponse scanQrCode(String qrCodeValue) {
        ProduceBatch batch = produceBatchRepository.findByQrCodeValue(qrCodeValue)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for this QR code"));
        return ProduceBatchMapper.toResponse(batch, getStages(batch));
    }

    private List<BatchStage> getStages(ProduceBatch batch) {
        return batchStageRepository.findByBatchOrderByCreatedAtAsc(batch);
    }

    private void requireOwnership(ProduceBatch batch, String farmerEmail) {
        if (!batch.getFarmer().getEmail().equals(farmerEmail)) {
            throw new UnauthorizedException("You do not own this produce batch");
        }
    }

    private ProduceBatch findBatchOrThrow(Long id) {
        return produceBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

package com.agrochain.backend.service;

import com.agrochain.backend.dto.CreateEquipmentRequest;
import com.agrochain.backend.dto.EquipmentResponse;
import com.agrochain.backend.dto.UpdateEquipmentRequest;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.Equipment;
import com.agrochain.backend.model.EquipmentCategory;
import com.agrochain.backend.model.NotificationType;
import com.agrochain.backend.model.Role;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.EquipmentRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FollowService followService;

    public Page<EquipmentResponse> getAllEquipment(String region, String district, EquipmentCategory category,
                                                    String query, Pageable pageable) {
        return equipmentRepository.search(region, district, category, query, pageable)
                .map(EquipmentMapper::toResponse);
    }

    public EquipmentResponse getEquipmentById(Long id) {
        Equipment equipment = findEquipmentOrThrow(id);
        return EquipmentMapper.toResponse(equipment);
    }

    public List<EquipmentResponse> getMyListings(String ownerEmail) {
        User owner = getUserOrThrow(ownerEmail);
        return equipmentRepository.findByOwner(owner).stream()
                .map(EquipmentMapper::toResponse)
                .toList();
    }

    public EquipmentResponse createEquipment(String ownerEmail, CreateEquipmentRequest request) {
        User owner = getUserOrThrow(ownerEmail);
        if (owner.getRole() != Role.EQUIPMENT_OWNER) {
            throw new UnauthorizedException("Only equipment owners can list equipment");
        }

        Equipment equipment = Equipment.builder()
                .owner(owner)
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .dailyRate(request.getDailyRate())
                .region(request.getRegion())
                .district(request.getDistrict())
                .imageUrl(request.getImageUrl())
                .isAvailable(true)
                .build();

        Equipment saved = equipmentRepository.save(equipment);

        followService.notifyFollowers(owner.getId(),
                owner.getFullName() + " listed a new " + saved.getName() + " - GHS " + saved.getDailyRate() + "/day",
                NotificationType.NEW_EQUIPMENT);

        return EquipmentMapper.toResponse(saved);
    }

    public EquipmentResponse updateEquipment(String ownerEmail, Long id, UpdateEquipmentRequest request) {
        Equipment equipment = findEquipmentOrThrow(id);
        requireOwnership(equipment, ownerEmail);
        BigDecimal previousRate = equipment.getDailyRate();

        if (request.getName() != null) {
            equipment.setName(request.getName());
        }
        if (request.getCategory() != null) {
            equipment.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            equipment.setDescription(request.getDescription());
        }
        if (request.getDailyRate() != null) {
            equipment.setDailyRate(request.getDailyRate());
        }
        if (request.getRegion() != null) {
            equipment.setRegion(request.getRegion());
        }
        if (request.getDistrict() != null) {
            equipment.setDistrict(request.getDistrict());
        }
        if (request.getImageUrl() != null) {
            equipment.setImageUrl(request.getImageUrl());
        }
        if (request.getIsAvailable() != null) {
            equipment.setAvailable(request.getIsAvailable());
        }

        Equipment saved = equipmentRepository.save(equipment);

        if (request.getDailyRate() != null && previousRate.compareTo(saved.getDailyRate()) != 0) {
            User owner = saved.getOwner();
            followService.notifyFollowers(owner.getId(),
                    owner.getFullName() + " updated " + saved.getName() + " price to GHS " + saved.getDailyRate() + "/day",
                    NotificationType.PRICE_CHANGE);
        }

        return EquipmentMapper.toResponse(saved);
    }

    public void deleteEquipment(String ownerEmail, Long id) {
        Equipment equipment = findEquipmentOrThrow(id);
        requireOwnership(equipment, ownerEmail);
        equipmentRepository.delete(equipment);
    }

    private void requireOwnership(Equipment equipment, String ownerEmail) {
        if (!equipment.getOwner().getEmail().equals(ownerEmail)) {
            throw new UnauthorizedException("You do not own this equipment listing");
        }
    }

    private Equipment findEquipmentOrThrow(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

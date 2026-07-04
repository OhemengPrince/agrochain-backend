package com.agrochain.backend.controller;

import com.agrochain.backend.dto.CreateEquipmentRequest;
import com.agrochain.backend.dto.EquipmentResponse;
import com.agrochain.backend.dto.UpdateEquipmentRequest;
import com.agrochain.backend.model.EquipmentCategory;
import com.agrochain.backend.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<Page<EquipmentResponse>> getAllEquipment(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) EquipmentCategory category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(equipmentService.getAllEquipment(region, district, category, query, pageable));
    }

    @GetMapping("/my-listings")
    public ResponseEntity<List<EquipmentResponse>> getMyListings(Authentication authentication) {
        return ResponseEntity.ok(equipmentService.getMyListings(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(id));
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(Authentication authentication,
                                                              @Valid @RequestBody CreateEquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.createEquipment(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> updateEquipment(Authentication authentication,
                                                              @PathVariable Long id,
                                                              @RequestBody UpdateEquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.updateEquipment(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(Authentication authentication, @PathVariable Long id) {
        equipmentService.deleteEquipment(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}

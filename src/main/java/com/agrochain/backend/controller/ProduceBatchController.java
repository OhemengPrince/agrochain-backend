package com.agrochain.backend.controller;

import com.agrochain.backend.dto.AddStageRequest;
import com.agrochain.backend.dto.BatchResponse;
import com.agrochain.backend.dto.CreateBatchRequest;
import com.agrochain.backend.dto.UpdateBatchStatusRequest;
import com.agrochain.backend.service.ProduceBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produce")
@RequiredArgsConstructor
public class ProduceBatchController {

    private final ProduceBatchService produceBatchService;

    @PostMapping("/batches")
    public ResponseEntity<BatchResponse> createBatch(Authentication authentication,
                                                       @Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.ok(produceBatchService.createBatch(authentication.getName(), request));
    }

    @GetMapping("/batches/mine")
    public ResponseEntity<List<BatchResponse>> getMyBatches(Authentication authentication) {
        return ResponseEntity.ok(produceBatchService.getMyBatches(authentication.getName()));
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<BatchResponse> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(produceBatchService.getBatchById(id));
    }

    @PostMapping("/batches/{id}/stages")
    public ResponseEntity<BatchResponse> addStage(Authentication authentication,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody AddStageRequest request) {
        return ResponseEntity.ok(produceBatchService.addStage(authentication.getName(), id, request));
    }

    @PatchMapping("/batches/{id}/status")
    public ResponseEntity<BatchResponse> updateStatus(Authentication authentication,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody UpdateBatchStatusRequest request) {
        return ResponseEntity.ok(produceBatchService.updateStatus(authentication.getName(), id, request.getStatus()));
    }

    @GetMapping("/catalogue")
    public ResponseEntity<List<BatchResponse>> getCatalogue(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String cropName,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(produceBatchService.getCatalogue(region, district, cropName, query, size));
    }

    @GetMapping("/scan")
    public ResponseEntity<BatchResponse> scanQrCode(@RequestParam String code) {
        return ResponseEntity.ok(produceBatchService.scanQrCode(code));
    }
}

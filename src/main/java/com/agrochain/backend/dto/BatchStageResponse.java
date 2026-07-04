package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStageResponse {

    private Long id;
    private String stageName;
    private String description;
    private String location;
    private LocalDateTime createdAt;
}

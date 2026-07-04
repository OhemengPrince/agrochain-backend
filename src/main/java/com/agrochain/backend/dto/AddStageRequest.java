package com.agrochain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStageRequest {

    @NotBlank(message = "Stage name is required")
    private String stageName;

    private String description;

    private String location;
}

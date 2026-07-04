package com.agrochain.backend.dto;

import com.agrochain.backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private String region;
    private String district;
    private String profilePhotoUrl;
    private boolean isVerified;
}

package com.agrochain.backend.service;

import com.agrochain.backend.dto.UserDto;
import com.agrochain.backend.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .region(user.getRegion())
                .district(user.getDistrict())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .isVerified(user.isVerified())
                .build();
    }
}

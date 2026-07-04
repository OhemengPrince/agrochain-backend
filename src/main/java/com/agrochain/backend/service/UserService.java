package com.agrochain.backend.service;

import com.agrochain.backend.dto.UpdateProfileRequest;
import com.agrochain.backend.dto.UserDto;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserMapper.toDto(user);
    }

    public UserDto updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());
        }
        if (request.getDistrict() != null) {
            user.setDistrict(request.getDistrict());
        }

        User savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }
}

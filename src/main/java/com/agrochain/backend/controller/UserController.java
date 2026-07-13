package com.agrochain.backend.controller;

import com.agrochain.backend.dto.TopRatedUserDto;
import com.agrochain.backend.dto.UpdateProfileRequest;
import com.agrochain.backend.dto.UserDto;
import com.agrochain.backend.model.Role;
import com.agrochain.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateProfile(Authentication authentication,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<TopRatedUserDto>> getTopRatedUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.getTopRatedUsers(role, limit));
    }
}

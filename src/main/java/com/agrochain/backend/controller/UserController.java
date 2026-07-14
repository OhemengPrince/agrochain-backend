package com.agrochain.backend.controller;

import com.agrochain.backend.dto.PublicUserDto;
import com.agrochain.backend.dto.TopRatedUserDto;
import com.agrochain.backend.dto.UpdatePhotoUrlRequest;
import com.agrochain.backend.dto.UpdateProfileRequest;
import com.agrochain.backend.dto.UserDto;
import com.agrochain.backend.model.Role;
import com.agrochain.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/me/photo")
    public ResponseEntity<UserDto> uploadProfilePhoto(Authentication authentication,
                                                        @RequestParam("photo") MultipartFile photo) {
        return ResponseEntity.ok(userService.uploadProfilePhoto(authentication.getName(), photo));
    }

    @PutMapping("/me/photo-url")
    public ResponseEntity<UserDto> updateProfilePhotoUrl(Authentication authentication,
                                                          @Valid @RequestBody UpdatePhotoUrlRequest request) {
        return ResponseEntity.ok(userService.updateProfilePhotoUrl(authentication.getName(), request.getPhotoUrl()));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<TopRatedUserDto>> getTopRatedUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.getTopRatedUsers(role, limit));
    }

    // Public — only ever returns PublicUserDto's narrow, safe field set.
    // Used by the mobile Top Rated carousel to enrich profiles without a token.
    @GetMapping("/{id}")
    public ResponseEntity<PublicUserDto> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getPublicProfile(id));
    }
}

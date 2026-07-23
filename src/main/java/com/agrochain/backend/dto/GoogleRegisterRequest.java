package com.agrochain.backend.dto;

import com.agrochain.backend.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// email/fullName/profilePhotoUrl come from Google's verified token
// response, not the request body — see GoogleLoginRequest.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleRegisterRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    @NotNull(message = "Role is required")
    private Role role;

    private String region;
    private String district;
    private String phoneNumber;
}

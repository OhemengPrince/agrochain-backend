package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Either an existing-user login result (token + user, newUser=false) or a
// prompt for the frontend to collect role/region/etc. before calling
// /auth/google-register (newUser=true, token/user null).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginResponse {

    private boolean newUser;
    private String token;
    private UserDto user;

    private String email;
    private String fullName;
    private String profilePhotoUrl;
}

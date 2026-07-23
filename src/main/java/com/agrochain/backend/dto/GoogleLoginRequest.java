package com.agrochain.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// email/fullName/profilePhotoUrl are deliberately NOT accepted here — they
// come from Google's verified token response, never trusted from the
// client, since login must not let a caller assert an arbitrary email.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}

package com.agrochain.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Maps the flat JSON returned by Google's tokeninfo endpoint
// (https://oauth2.googleapis.com/tokeninfo?id_token=...). "aud" must be
// checked against our configured OAuth client id, and "emailVerified"
// must be "true" — that's what makes this a verification rather than a
// simple decode.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTokenInfoResponse {

    private String aud;
    private String sub;
    private String email;

    @JsonProperty("email_verified")
    private String emailVerified;

    private String name;
    private String picture;
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}

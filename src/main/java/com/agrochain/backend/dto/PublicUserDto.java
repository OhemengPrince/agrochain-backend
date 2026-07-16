package com.agrochain.backend.dto;

import com.agrochain.backend.model.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Deliberately narrow — this is served to unauthenticated callers (Top Rated
// carousel enrichment), so it must never carry email, phoneNumber, or any
// other field that isn't safe to show to the public.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"verified"})
public class PublicUserDto {

    private Long id;
    private String fullName;
    private Role role;
    private String region;
    private String district;
    private String profilePhotoUrl;

    @JsonProperty("isVerified")
    private boolean isVerified;

    private Double averageRating;
    private Long followerCount;
    private Long followingCount;

    // Nullable — null means the caller is unauthenticated, not "not following".
    @JsonProperty("isFollowing")
    private Boolean isFollowing;
}

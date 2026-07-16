package com.agrochain.backend.controller;

import com.agrochain.backend.dto.FollowCountsResponse;
import com.agrochain.backend.dto.FollowStatusResponse;
import com.agrochain.backend.dto.PublicUserDto;
import com.agrochain.backend.service.FollowService;
import com.agrochain.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final UserService userService;

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(Authentication authentication, @PathVariable Long id) {
        Long followerId = currentUserId(authentication);
        followService.followUser(followerId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(Authentication authentication, @PathVariable Long id) {
        Long followerId = currentUserId(authentication);
        followService.unfollowUser(followerId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<List<PublicUserDto>> getFollowers(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(followService.getFollowers(id, resolveViewerId(authentication)));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<PublicUserDto>> getFollowing(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(followService.getFollowing(id, resolveViewerId(authentication)));
    }

    @GetMapping("/{id}/follow-status")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(Authentication authentication, @PathVariable Long id) {
        Long followerId = currentUserId(authentication);
        boolean isFollowing = followService.isFollowing(followerId, id);
        return ResponseEntity.ok(FollowStatusResponse.builder().isFollowing(isFollowing).build());
    }

    @GetMapping("/{id}/follow-counts")
    public ResponseEntity<FollowCountsResponse> getFollowCounts(@PathVariable Long id) {
        return ResponseEntity.ok(FollowCountsResponse.builder()
                .followerCount(followService.getFollowerCount(id))
                .followingCount(followService.getFollowingCount(id))
                .build());
    }

    private Long currentUserId(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName()).getId();
    }

    // Public endpoints stay reachable without a token, but if a valid one is
    // attached, JwtAuthFilter still populates Authentication — Spring Security's
    // default anonymous auth otherwise fills it with a non-null "anonymousUser"
    // principal, so that has to be filtered out explicitly rather than just
    // null-checked.
    private Long resolveViewerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.getCurrentUser(authentication.getName()).getId();
    }
}

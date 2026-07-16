package com.agrochain.backend.service;

import com.agrochain.backend.dto.PublicUserDto;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.model.Follow;
import com.agrochain.backend.model.NotificationType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.FollowRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    @Transactional
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return;
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        followRepository.save(Follow.builder().follower(follower).following(following).build());

        notificationService.createNotification(followingId, "New Follower",
                follower.getFullName() + " started following you!", NotificationType.NEW_FOLLOWER);
    }

    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public List<PublicUserDto> getFollowers(Long userId, Long viewerId) {
        return followRepository.findAllByFollowingId(userId).stream()
                .map(Follow::getFollower)
                .map(user -> userService.toPublicUserDto(user, viewerId))
                .toList();
    }

    public List<PublicUserDto> getFollowing(Long userId, Long viewerId) {
        return followRepository.findAllByFollowerId(userId).stream()
                .map(Follow::getFollowing)
                .map(user -> userService.toPublicUserDto(user, viewerId))
                .toList();
    }

    public long getFollowerCount(Long userId) {
        return followRepository.countByFollowingId(userId);
    }

    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    // Fans out a notification to every follower of userId — used when a
    // followed user lists new equipment/produce/a listing, changes a price,
    // or has a booking accepted.
    public void notifyFollowers(Long userId, String message, NotificationType type) {
        List<Follow> followers = followRepository.findAllByFollowingId(userId);
        for (Follow follow : followers) {
            notificationService.createNotification(follow.getFollower().getId(), titleFor(type), message, type);
        }
    }

    private String titleFor(NotificationType type) {
        return switch (type) {
            case NEW_EQUIPMENT -> "New Equipment";
            case NEW_LISTING -> "New Listing";
            case NEW_PRODUCE -> "New Produce";
            case PRICE_CHANGE -> "Price Update";
            case BOOKING_ACCEPTED -> "Booking Accepted";
            case NEW_FOLLOWER -> "New Follower";
            default -> "Update";
        };
    }
}

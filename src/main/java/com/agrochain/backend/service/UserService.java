package com.agrochain.backend.service;

import com.agrochain.backend.dto.AverageRatingResponse;
import com.agrochain.backend.dto.PublicUserDto;
import com.agrochain.backend.dto.ReviewResponse;
import com.agrochain.backend.dto.TopRatedUserDto;
import com.agrochain.backend.dto.UpdateProfileRequest;
import com.agrochain.backend.dto.UserDto;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.model.ProduceBatch;
import com.agrochain.backend.model.Review;
import com.agrochain.backend.model.Role;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.EquipmentRepository;
import com.agrochain.backend.repository.FollowRepository;
import com.agrochain.backend.repository.ProduceBatchRepository;
import com.agrochain.backend.repository.ReviewRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProduceBatchRepository produceBatchRepository;
    private final ReviewRepository reviewRepository;
    private final FollowRepository followRepository;
    private final FileStorageService fileStorageService;

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
        if (request.getProfilePhotoUrl() != null) {
            log.info("Saving profilePhotoUrl: {}", request.getProfilePhotoUrl());
            user.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }

        User savedUser = userRepository.save(user);
        log.info("Saved user profilePhotoUrl: {}", savedUser.getProfilePhotoUrl());
        return UserMapper.toDto(savedUser);
    }

    public UserDto uploadProfilePhoto(String email, MultipartFile photo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String filename = fileStorageService.storeImage(photo);
        String fileUrl = "/api/files/" + filename;
        log.info("Saving profilePhotoUrl: {}", fileUrl);
        user.setProfilePhotoUrl(fileUrl);

        User savedUser = userRepository.save(user);
        log.info("Saved user profilePhotoUrl: {}", savedUser.getProfilePhotoUrl());
        return UserMapper.toDto(savedUser);
    }

    public UserDto updateProfilePhotoUrl(String email, String photoUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setProfilePhotoUrl(photoUrl);

        User savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }

    // Public — served to unauthenticated callers, so only ever return fields
    // that are safe to expose (see PublicUserDto for the exact contract).
    // viewerId is nullable — null means the caller is unauthenticated, which
    // PublicUserDto.isFollowing distinguishes from "authenticated but not following".
    public PublicUserDto getPublicProfile(Long id, Long viewerId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toPublicUserDto(user, viewerId);
    }

    PublicUserDto toPublicUserDto(User user, Long viewerId) {
        List<Review> reviews = reviewRepository.findByReviewee(user);
        Double averageRating = reviews.isEmpty() ? null
                : Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0;

        long followerCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());
        Boolean isFollowing = viewerId == null ? null
                : followRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId());

        return UserMapper.toPublicUserDto(user, averageRating, reviews.size(), followerCount, followingCount, isFollowing);
    }

    public List<ReviewResponse> getUserReviews(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return reviewRepository.findByRevieweeOrderByCreatedAtDesc(user).stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .reviewerName(review.getReviewer().getFullName())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build())
                .toList();
    }

    public AverageRatingResponse getAverageRating(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Review> reviews = reviewRepository.findByReviewee(user);
        Double averageRating = reviews.isEmpty() ? null
                : Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0;

        return AverageRatingResponse.builder()
                .averageRating(averageRating)
                .totalReviews(reviews.size())
                .build();
    }

    // Public — same safe field set as getPublicProfile. Only verified users
    // are eligible, matching getTopRatedUsers' visibility rule.
    public List<PublicUserDto> getRecentUsers(int limit, Long viewerId) {
        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findByIsVerifiedTrueOrderByCreatedAtDesc(pageable).stream()
                .map(user -> toPublicUserDto(user, viewerId))
                .toList();
    }

    public List<TopRatedUserDto> getTopRatedUsers(Role role, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> rows = userRepository.findTopRatedUsers(role, pageable);

        return rows.stream().map(row -> {
            User user = (User) row[0];
            double averageRating = ((Number) row[1]).doubleValue();
            long totalReviews = ((Number) row[2]).longValue();

            return TopRatedUserDto.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .region(user.getRegion())
                    .district(user.getDistrict())
                    .profilePhotoUrl(user.getProfilePhotoUrl())
                    .averageRating(Math.round(averageRating * 10) / 10.0)
                    .totalReviews(totalReviews)
                    .isVerified(true)
                    .speciality(resolveSpeciality(user))
                    .build();
        }).toList();
    }

    private String resolveSpeciality(User user) {
        if (user.getRole() == Role.FARMER) {
            List<String> crops = produceBatchRepository.findByFarmer(user).stream()
                    .map(ProduceBatch::getCropName)
                    .distinct()
                    .toList();
            return crops.isEmpty() ? null : String.join(", ", crops);
        }
        if (user.getRole() == Role.EQUIPMENT_OWNER) {
            List<String> categories = equipmentRepository.findByOwner(user).stream()
                    .map(equipment -> equipment.getCategory().name())
                    .distinct()
                    .toList();
            return categories.isEmpty() ? null : String.join(", ", categories);
        }
        return null;
    }
}

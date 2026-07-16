package com.agrochain.backend.repository;

import com.agrochain.backend.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findAllByFollowingId(Long followingId);

    List<Follow> findAllByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}

package com.agrochain.backend.repository;

import com.agrochain.backend.model.ItemReaction;
import com.agrochain.backend.model.ReactionTargetType;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemReactionRepository extends JpaRepository<ItemReaction, Long> {

    List<ItemReaction> findByTargetTypeAndTargetId(ReactionTargetType targetType, Long targetId);

    List<ItemReaction> findByTargetTypeAndTargetIdIn(ReactionTargetType targetType, List<Long> targetIds);

    Optional<ItemReaction> findByTargetTypeAndTargetIdAndUser(ReactionTargetType targetType, Long targetId, User user);

    void deleteByTargetTypeAndTargetId(ReactionTargetType targetType, Long targetId);
}

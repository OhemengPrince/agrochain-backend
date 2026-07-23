package com.agrochain.backend.service;

import com.agrochain.backend.dto.ReactionSummary;
import com.agrochain.backend.model.ItemReaction;
import com.agrochain.backend.model.ReactionTargetType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.ItemReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Shared reaction logic for both item comments and chat messages — one
// ItemReaction table, discriminated by targetType, so this doesn't need to be
// duplicated per feature.
@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ItemReactionRepository itemReactionRepository;

    // Re-picking the same emoji removes it (toggle off); picking a different
    // emoji replaces the user's existing reaction on that target.
    @Transactional
    public void react(ReactionTargetType targetType, Long targetId, User user, String emoji) {
        itemReactionRepository.findByTargetTypeAndTargetIdAndUser(targetType, targetId, user)
                .ifPresentOrElse(existing -> {
                    if (existing.getEmoji().equals(emoji)) {
                        itemReactionRepository.delete(existing);
                    } else {
                        existing.setEmoji(emoji);
                        itemReactionRepository.save(existing);
                    }
                }, () -> itemReactionRepository.save(ItemReaction.builder()
                        .targetType(targetType)
                        .targetId(targetId)
                        .user(user)
                        .emoji(emoji)
                        .build()));
    }

    public List<ReactionSummary> getSummary(ReactionTargetType targetType, Long targetId) {
        return summarize(itemReactionRepository.findByTargetTypeAndTargetId(targetType, targetId));
    }

    public String getMyReaction(ReactionTargetType targetType, Long targetId, User user) {
        return itemReactionRepository.findByTargetTypeAndTargetIdAndUser(targetType, targetId, user)
                .map(ItemReaction::getEmoji)
                .orElse(null);
    }

    // Batches reaction lookups for a list of comments/messages in one query
    // instead of one-per-item, keeping list endpoints from N+1'ing.
    public Map<Long, List<ItemReaction>> getSummaryBatch(ReactionTargetType targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) return Map.of();
        return itemReactionRepository.findByTargetTypeAndTargetIdIn(targetType, targetIds).stream()
                .collect(Collectors.groupingBy(ItemReaction::getTargetId));
    }

    public List<ReactionSummary> summarize(List<ItemReaction> reactions) {
        return reactions.stream()
                .collect(Collectors.groupingBy(ItemReaction::getEmoji, Collectors.counting()))
                .entrySet().stream()
                .map(e -> ReactionSummary.builder().emoji(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    public void deleteAllFor(ReactionTargetType targetType, Long targetId) {
        itemReactionRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
    }
}

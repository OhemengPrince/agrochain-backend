package com.agrochain.backend.service;

import com.agrochain.backend.dto.ItemCommentResponse;
import com.agrochain.backend.dto.ReactionSummary;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.ItemComment;
import com.agrochain.backend.model.ItemReaction;
import com.agrochain.backend.model.ItemType;
import com.agrochain.backend.model.ReactionTargetType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.ItemCommentRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemCommentService {

    private final ItemCommentRepository itemCommentRepository;
    private final UserRepository userRepository;
    private final ReactionService reactionService;

    @Transactional(readOnly = true)
    public List<ItemCommentResponse> list(ItemType itemType, Long itemId, String viewerEmail) {
        List<ItemComment> comments = itemCommentRepository.findByItemTypeAndItemIdOrderByCreatedAtAsc(itemType, itemId);
        User viewer = viewerEmail != null ? getUserOrThrow(viewerEmail) : null;

        Map<Long, List<ItemReaction>> reactionsByComment = reactionService.getSummaryBatch(
                ReactionTargetType.COMMENT, comments.stream().map(ItemComment::getId).toList());

        return comments.stream()
                .map(c -> toResponse(c, reactionsByComment.getOrDefault(c.getId(), List.of()), viewer))
                .toList();
    }

    @Transactional
    public ItemCommentResponse create(ItemType itemType, Long itemId, String authorEmail, String text, Long parentId) {
        User author = getUserOrThrow(authorEmail);
        ItemComment comment = ItemComment.builder()
                .itemType(itemType)
                .itemId(itemId)
                .author(author)
                .text(text)
                .parentId(parentId)
                .build();
        ItemComment saved = itemCommentRepository.save(comment);
        return toResponse(saved, List.of(), author);
    }

    @Transactional
    public void delete(Long commentId, String requesterEmail) {
        ItemComment comment = itemCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!comment.getAuthor().getEmail().equals(requesterEmail)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }
        reactionService.deleteAllFor(ReactionTargetType.COMMENT, commentId);
        itemCommentRepository.delete(comment);
    }

    @Transactional
    public ItemCommentResponse react(Long commentId, String userEmail, String emoji) {
        ItemComment comment = itemCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        User user = getUserOrThrow(userEmail);
        reactionService.react(ReactionTargetType.COMMENT, commentId, user, emoji);
        List<ItemReaction> reactions = reactionService.getSummaryBatch(ReactionTargetType.COMMENT, List.of(commentId))
                .getOrDefault(commentId, List.of());
        return toResponse(comment, reactions, user);
    }

    private ItemCommentResponse toResponse(ItemComment comment, List<ItemReaction> reactions, User viewer) {
        List<ReactionSummary> summary = reactionService.summarize(reactions);
        String myReaction = viewer == null ? null : reactions.stream()
                .filter(r -> r.getUser().getId().equals(viewer.getId()))
                .map(ItemReaction::getEmoji)
                .findFirst()
                .orElse(null);

        return ItemCommentResponse.builder()
                .id(comment.getId())
                .itemType(comment.getItemType())
                .itemId(comment.getItemId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .text(comment.getText())
                .parentId(comment.getParentId())
                .createdAt(comment.getCreatedAt())
                .reactions(summary)
                .myReaction(myReaction)
                .build();
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

package com.agrochain.backend.dto;

import com.agrochain.backend.model.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCommentResponse {

    private Long id;
    private ItemType itemType;
    private Long itemId;
    private Long authorId;
    private String authorName;
    private String text;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<ReactionSummary> reactions;
    private String myReaction;
}

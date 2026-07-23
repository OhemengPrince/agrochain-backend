package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Broadcast to everyone in the item room on a reaction change. Deliberately
// has no "myReaction" field — that's viewer-specific and only meaningful in
// the direct response to the user who made the request, not a room broadcast.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionBroadcast {

    private Long commentId;
    private List<ReactionSummary> reactions;
}

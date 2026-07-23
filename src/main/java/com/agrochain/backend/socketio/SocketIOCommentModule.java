package com.agrochain.backend.socketio;

import com.agrochain.backend.dto.CommentReactionBroadcast;
import com.agrochain.backend.dto.ItemCommentResponse;
import com.agrochain.backend.dto.JoinItemEvent;
import com.agrochain.backend.dto.SocketDeleteCommentEvent;
import com.agrochain.backend.dto.SocketItemCommentEvent;
import com.agrochain.backend.dto.SocketReactCommentEvent;
import com.agrochain.backend.model.ReactionTargetType;
import com.agrochain.backend.service.ItemCommentService;
import com.agrochain.backend.service.ReactionService;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

// Realtime comment sync — same shape as SocketIOChatModule (room-per-resource,
// persistence delegated to the existing service layer, this module only does
// room membership + broadcast). Room key is per marketplace/equipment item so
// everyone currently viewing that item's comments sees new ones instantly,
// regardless of which account posted them.
@Component
@RequiredArgsConstructor
@Slf4j
public class SocketIOCommentModule {

    private final SocketIOServer server;
    private final ItemCommentService itemCommentService;
    private final ReactionService reactionService;

    @PostConstruct
    public void registerHandlers() {
        server.addEventListener("join_item", JoinItemEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            client.joinRoom(itemChannel(data.getItemType().name(), data.getItemId()));
            log.info("[SocketIO] {} joined {}", email, itemChannel(data.getItemType().name(), data.getItemId()));
        });

        server.addEventListener("new_comment", SocketItemCommentEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            try {
                ItemCommentResponse saved = itemCommentService.create(
                        data.getItemType(), data.getItemId(), email, data.getText(), data.getParentId());
                server.getRoomOperations(itemChannel(data.getItemType().name(), data.getItemId())).sendEvent("new_comment", saved);
            } catch (Exception e) {
                log.warn("[SocketIO] new_comment rejected: user={}, reason={}", email, e.getMessage());
                client.sendEvent("error", Map.of("event", "new_comment", "message", errorMessage(e)));
            }
        });

        server.addEventListener("delete_comment", SocketDeleteCommentEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            try {
                itemCommentService.delete(data.getCommentId(), email);
                server.getRoomOperations(itemChannel(data.getItemType().name(), data.getItemId()))
                        .sendEvent("comment_deleted", Map.of("commentId", data.getCommentId()));
            } catch (Exception e) {
                log.warn("[SocketIO] delete_comment rejected: user={}, reason={}", email, e.getMessage());
                client.sendEvent("error", Map.of("event", "delete_comment", "message", errorMessage(e)));
            }
        });

        server.addEventListener("react_comment", SocketReactCommentEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            try {
                itemCommentService.react(data.getCommentId(), email, data.getEmoji());
                CommentReactionBroadcast broadcast = CommentReactionBroadcast.builder()
                        .commentId(data.getCommentId())
                        .reactions(reactionService.getSummary(ReactionTargetType.COMMENT, data.getCommentId()))
                        .build();
                server.getRoomOperations(itemChannel(data.getItemType().name(), data.getItemId()))
                        .sendEvent("comment_reaction", broadcast);
            } catch (Exception e) {
                log.warn("[SocketIO] react_comment rejected: user={}, reason={}", email, e.getMessage());
                client.sendEvent("error", Map.of("event", "react_comment", "message", errorMessage(e)));
            }
        });

        log.info("[SocketIO] Comment event handlers registered: join_item, new_comment, delete_comment, react_comment");
    }

    private String itemChannel(String itemType, Long itemId) {
        return "item-" + itemType + "-" + itemId;
    }

    private String errorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Request could not be processed";
    }
}

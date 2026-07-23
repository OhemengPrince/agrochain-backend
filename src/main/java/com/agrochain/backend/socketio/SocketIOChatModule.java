package com.agrochain.backend.socketio;

import com.agrochain.backend.dto.ChatMessageResponse;
import com.agrochain.backend.dto.JoinRoomEvent;
import com.agrochain.backend.dto.MarkReadEvent;
import com.agrochain.backend.dto.MessageReactionBroadcast;
import com.agrochain.backend.dto.ReactionSummary;
import com.agrochain.backend.dto.SocketDeleteMessageEvent;
import com.agrochain.backend.dto.SocketReactMessageEvent;
import com.agrochain.backend.dto.SocketSendMessageEvent;
import com.agrochain.backend.service.ChatService;

import java.util.List;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Registers the live-chat Socket.IO event handlers. Kept separate from
 * SocketIOConfig so the transport/auth wiring and the chat business logic
 * don't live in the same class. All persistence is delegated to the existing
 * ChatService — this module only does room-membership checks and broadcast.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocketIOChatModule {

    private final SocketIOServer server;
    private final ChatService chatService;

    @PostConstruct
    public void registerHandlers() {
        server.addEventInterceptor((client, eventName, args, ackRequest) ->
                log.info("[SocketIO] RAW EVENT RECEIVED: name={}, args={}, sessionId={}, email={}",
                        eventName, args, client.getSessionId(), client.get("email")));

        server.addEventListener("join_room", JoinRoomEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            log.info("[SocketIO] join_room: user={}, roomId={}", email, data.getRoomId());
            try {
                chatService.verifyParticipant(email, data.getRoomId());
                client.joinRoom(roomChannel(data.getRoomId()));
                log.info("[SocketIO] {} joined {}", email, roomChannel(data.getRoomId()));
            } catch (Exception e) {
                log.warn("[SocketIO] join_room rejected: user={}, roomId={}, reason={}", email, data.getRoomId(), e.getMessage());
                client.sendEvent("error", Map.of("event", "join_room", "message", errorMessage(e)));
            }
        });

        server.addEventListener("send_message", SocketSendMessageEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            log.info("[SocketIO] send_message: user={}, roomId={}, contentPreview={}",
                    email, data.getRoomId(), preview(data.getContent()));
            try {
                ChatMessageResponse saved = chatService.saveMessage(data.getRoomId(), email, data.getContent(),
                        data.getAudioUrl(), data.getAudioDuration(), data.getType(), data.getReplyToId());
                server.getRoomOperations(roomChannel(data.getRoomId())).sendEvent("new_message", saved);
                log.info("[SocketIO] message {} broadcast to {}", saved.getId(), roomChannel(data.getRoomId()));
            } catch (Exception e) {
                log.warn("[SocketIO] send_message rejected: user={}, roomId={}, reason={}", email, data.getRoomId(), e.getMessage());
                client.sendEvent("error", Map.of("event", "send_message", "message", errorMessage(e)));
            }
        });

        server.addEventListener("delete_message", SocketDeleteMessageEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            try {
                chatService.deleteMessage(data.getMessageId(), email);
                server.getRoomOperations(roomChannel(data.getRoomId()))
                        .sendEvent("message_deleted", Map.of("messageId", data.getMessageId()));
            } catch (Exception e) {
                log.warn("[SocketIO] delete_message rejected: user={}, roomId={}, reason={}", email, data.getRoomId(), e.getMessage());
                client.sendEvent("error", Map.of("event", "delete_message", "message", errorMessage(e)));
            }
        });

        server.addEventListener("react_message", SocketReactMessageEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            try {
                List<ReactionSummary> reactions = chatService.reactToMessage(data.getMessageId(), email, data.getEmoji());
                MessageReactionBroadcast broadcast = MessageReactionBroadcast.builder()
                        .messageId(data.getMessageId())
                        .reactions(reactions)
                        .build();
                server.getRoomOperations(roomChannel(data.getRoomId())).sendEvent("message_reaction", broadcast);
            } catch (Exception e) {
                log.warn("[SocketIO] react_message rejected: user={}, roomId={}, reason={}", email, data.getRoomId(), e.getMessage());
                client.sendEvent("error", Map.of("event", "react_message", "message", errorMessage(e)));
            }
        });

        server.addEventListener("mark_read", MarkReadEvent.class, (client, data, ackSender) -> {
            String email = client.get("email");
            log.info("[SocketIO] mark_read: user={}, roomId={}", email, data.getRoomId());
            try {
                chatService.markMessagesAsRead(email, data.getRoomId());
            } catch (Exception e) {
                log.warn("[SocketIO] mark_read failed: user={}, roomId={}, reason={}", email, data.getRoomId(), e.getMessage());
                client.sendEvent("error", Map.of("event", "mark_read", "message", errorMessage(e)));
            }
        });

        log.info("[SocketIO] Chat event handlers registered: join_room, send_message, mark_read, delete_message, react_message");
    }

    private String roomChannel(Long roomId) {
        return "room-" + roomId;
    }

    private String errorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Request could not be processed";
    }

    private String preview(String content) {
        if (content == null) return null;
        return content.length() > 60 ? content.substring(0, 60) + "…" : content;
    }
}

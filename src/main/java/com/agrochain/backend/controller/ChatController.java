package com.agrochain.backend.controller;

import com.agrochain.backend.dto.*;
import com.agrochain.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getRooms(Authentication authentication) {
        return ResponseEntity.ok(chatService.getRooms(authentication.getName()));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> getOrCreateRoom(Authentication authentication,
                                                             @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(chatService.getOrCreateRoom(authentication.getName(), request.getOtherUserId()));
    }

    @GetMapping("/rooms/{id}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(authentication.getName(), id));
    }

    @PatchMapping("/rooms/{id}/read")
    public ResponseEntity<Map<String, String>> markMessagesAsRead(Authentication authentication, @PathVariable Long id) {
        chatService.markMessagesAsRead(authentication.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Messages marked as read."));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(Authentication authentication, @PathVariable Long id) {
        chatService.deleteMessage(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/messages/{id}/react")
    public ResponseEntity<List<ReactionSummary>> reactToMessage(Authentication authentication, @PathVariable Long id,
                                                                  @Valid @RequestBody ReactionRequest request) {
        return ResponseEntity.ok(chatService.reactToMessage(id, authentication.getName(), request.getEmoji()));
    }

    @MessageMapping("/chat/{roomId}")
    public void receiveMessage(@DestinationVariable Long roomId, @Payload SendMessageRequest request, Principal principal) {
        ChatMessageResponse saved = chatService.saveMessage(roomId, principal.getName(), request.getContent());
        ChatMessageWebSocket payload = ChatMessageWebSocket.builder()
                .roomId(roomId)
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, payload);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId + "/notify", payload);
    }
}

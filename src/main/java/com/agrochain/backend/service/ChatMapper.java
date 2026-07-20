package com.agrochain.backend.service;

import com.agrochain.backend.dto.ChatMessageResponse;
import com.agrochain.backend.dto.ChatMessageWebSocket;
import com.agrochain.backend.dto.ChatRoomResponse;
import com.agrochain.backend.model.ChatMessage;
import com.agrochain.backend.model.ChatRoom;

public final class ChatMapper {

    private ChatMapper() {
    }

    public static ChatRoomResponse toRoomResponse(ChatRoom room, long unreadCount) {
        return ChatRoomResponse.builder()
                .id(room.getId())
                .participant1(UserMapper.toDto(room.getParticipant1()))
                .participant2(UserMapper.toDto(room.getParticipant2()))
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }

    public static ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .content(message.getContent())
                .audioUrl(message.getAudioUrl())
                .audioDuration(message.getAudioDuration())
                .messageType(message.getMessageType())
                .isRead(message.isRead())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ChatMessageWebSocket toWebSocketMessage(ChatMessage message) {
        return ChatMessageWebSocket.builder()
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .content(message.getContent())
                .audioUrl(message.getAudioUrl())
                .audioDuration(message.getAudioDuration())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

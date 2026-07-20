package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageWebSocket {

    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private String audioUrl;
    private Integer audioDuration;
    private String messageType;
    private LocalDateTime createdAt;
}

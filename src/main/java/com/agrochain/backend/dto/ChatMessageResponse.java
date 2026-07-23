package com.agrochain.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties({"read"})
public class ChatMessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
    private String audioUrl;
    private Integer audioDuration;
    private String messageType;

    @JsonProperty("isRead")
    private boolean isRead;

    private Long replyToId;
    private boolean deleted;
    private List<ReactionSummary> reactions;
    private String myReaction;

    private LocalDateTime createdAt;
}

package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketSendMessageEvent {

    private Long roomId;
    private String content;
    private String audioUrl;
    private Integer audioDuration;
    private String type;
}

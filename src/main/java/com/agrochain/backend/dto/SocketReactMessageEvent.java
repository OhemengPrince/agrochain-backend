package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketReactMessageEvent {

    private Long roomId;
    private Long messageId;
    private String emoji;
}

package com.agrochain.backend.dto;

import com.agrochain.backend.model.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketDeleteCommentEvent {

    private ItemType itemType;
    private Long itemId;
    private Long commentId;
}

package com.agrochain.backend.dto;

import com.agrochain.backend.model.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketItemCommentEvent {

    private ItemType itemType;
    private Long itemId;
    private String text;
    private Long parentId;
}

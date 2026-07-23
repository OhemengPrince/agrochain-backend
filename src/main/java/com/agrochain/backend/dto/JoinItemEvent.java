package com.agrochain.backend.dto;

import com.agrochain.backend.model.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinItemEvent {

    private ItemType itemType;
    private Long itemId;
}

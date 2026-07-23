package com.agrochain.backend.repository;

import com.agrochain.backend.model.ItemComment;
import com.agrochain.backend.model.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCommentRepository extends JpaRepository<ItemComment, Long> {

    List<ItemComment> findByItemTypeAndItemIdOrderByCreatedAtAsc(ItemType itemType, Long itemId);
}

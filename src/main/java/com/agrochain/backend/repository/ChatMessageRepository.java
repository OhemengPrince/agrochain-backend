package com.agrochain.backend.repository;

import com.agrochain.backend.model.ChatMessage;
import com.agrochain.backend.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomOrderByCreatedAtAsc(ChatRoom room);

    List<ChatMessage> findByRoomAndIsReadFalse(ChatRoom room);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room = :room AND m.isRead = false")
    long countUnreadByRoom(@Param("room") ChatRoom room);
}

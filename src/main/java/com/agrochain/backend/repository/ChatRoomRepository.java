package com.agrochain.backend.repository;

import com.agrochain.backend.model.ChatRoom;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByParticipant1AndParticipant2(User participant1, User participant2);

    List<ChatRoom> findByParticipant1OrParticipant2(User participant1, User participant2);
}

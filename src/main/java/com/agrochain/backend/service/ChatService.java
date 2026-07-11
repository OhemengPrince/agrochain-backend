package com.agrochain.backend.service;

import com.agrochain.backend.dto.ChatMessageResponse;
import com.agrochain.backend.dto.ChatRoomResponse;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.ChatMessage;
import com.agrochain.backend.model.ChatRoom;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.ChatMessageRepository;
import com.agrochain.backend.repository.ChatRoomRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public List<ChatRoomResponse> getRooms(String userEmail) {
        User user = getUserOrThrow(userEmail);
        return chatRoomRepository.findByParticipant1OrParticipant2(user, user).stream()
                .map(room -> ChatMapper.toRoomResponse(room, unreadCountForOtherParty(room, user)))
                .toList();
    }

    public ChatRoomResponse getOrCreateRoom(String userEmail, Long otherUserId) {
        User user = getUserOrThrow(userEmail);
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getId().equals(otherUser.getId())) {
            throw new BadRequestException("You cannot start a chat with yourself");
        }

        ChatRoom room = chatRoomRepository.findByParticipant1AndParticipant2(user, otherUser)
                .or(() -> chatRoomRepository.findByParticipant1AndParticipant2(otherUser, user))
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .participant1(user)
                        .participant2(otherUser)
                        .build()));

        return ChatMapper.toRoomResponse(room, unreadCountForOtherParty(room, user));
    }

    public List<ChatMessageResponse> getMessages(String userEmail, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room, userEmail);

        return chatMessageRepository.findByRoomOrderByCreatedAtAsc(room).stream()
                .map(ChatMapper::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, String senderEmail, String content) {
        ChatRoom room = findRoomOrThrow(roomId);
        User sender = getUserOrThrow(senderEmail);
        requireParticipant(room, senderEmail);

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .isRead(false)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        room.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        return ChatMapper.toMessageResponse(saved);
    }

    @Transactional
    public void markMessagesAsRead(String userEmail, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room, userEmail);

        List<ChatMessage> unread = chatMessageRepository.findByRoomAndIsReadFalse(room).stream()
                .filter(message -> !message.getSender().getEmail().equals(userEmail))
                .toList();
        unread.forEach(message -> message.setRead(true));
        chatMessageRepository.saveAll(unread);
    }

    // Socket.IO handlers run on Netty event-loop threads with no HTTP request
    // (and therefore no open-in-view Hibernate session), so this needs its own
    // transaction to safely touch the lazy participant proxies.
    @Transactional(readOnly = true)
    public void verifyParticipant(String userEmail, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room, userEmail);
    }

    private long unreadCountForOtherParty(ChatRoom room, User user) {
        return chatMessageRepository.findByRoomAndIsReadFalse(room).stream()
                .filter(message -> !message.getSender().getId().equals(user.getId()))
                .count();
    }

    private void requireParticipant(ChatRoom room, String userEmail) {
        boolean isParticipant = room.getParticipant1().getEmail().equals(userEmail)
                || room.getParticipant2().getEmail().equals(userEmail);
        if (!isParticipant) {
            throw new UnauthorizedException("You are not part of this chat room");
        }
    }

    private ChatRoom findRoomOrThrow(Long id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

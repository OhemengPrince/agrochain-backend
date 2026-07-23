package com.agrochain.backend.service;

import com.agrochain.backend.dto.ChatMessageResponse;
import com.agrochain.backend.dto.ChatRoomResponse;
import com.agrochain.backend.dto.ReactionSummary;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.ChatMessage;
import com.agrochain.backend.model.ChatRoom;
import com.agrochain.backend.model.ItemReaction;
import com.agrochain.backend.model.ReactionTargetType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.ChatMessageRepository;
import com.agrochain.backend.repository.ChatRoomRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ReactionService reactionService;

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

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(String userEmail, Long roomId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room, userEmail);
        User viewer = getUserOrThrow(userEmail);

        List<ChatMessage> messages = chatMessageRepository.findByRoomOrderByCreatedAtAsc(room);
        Map<Long, List<ItemReaction>> reactionsByMessage = reactionService.getSummaryBatch(
                ReactionTargetType.MESSAGE, messages.stream().map(ChatMessage::getId).toList());

        return messages.stream()
                .map(m -> {
                    List<ItemReaction> reactions = reactionsByMessage.getOrDefault(m.getId(), List.of());
                    String myReaction = reactions.stream()
                            .filter(r -> r.getUser().getId().equals(viewer.getId()))
                            .map(ItemReaction::getEmoji)
                            .findFirst()
                            .orElse(null);
                    return ChatMapper.toMessageResponse(m, reactionService.summarize(reactions), myReaction);
                })
                .toList();
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, String senderEmail, String content) {
        return saveMessage(roomId, senderEmail, content, null, null, null, null);
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, String senderEmail, String content,
                                            String audioUrl, Integer audioDuration, String messageType) {
        return saveMessage(roomId, senderEmail, content, audioUrl, audioDuration, messageType, null);
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long roomId, String senderEmail, String content,
                                            String audioUrl, Integer audioDuration, String messageType, Long replyToId) {
        ChatRoom room = findRoomOrThrow(roomId);
        User sender = getUserOrThrow(senderEmail);
        requireParticipant(room, senderEmail);

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .audioUrl(audioUrl)
                .audioDuration(audioDuration)
                .messageType(messageType != null ? messageType : "TEXT")
                .isRead(false)
                .replyToId(replyToId)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        room.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        return ChatMapper.toMessageResponse(saved);
    }

    @Transactional
    public void deleteMessage(Long messageId, String requesterEmail) {
        ChatMessage message = findMessageOrThrow(messageId);
        if (!message.getSender().getEmail().equals(requesterEmail)) {
            throw new UnauthorizedException("You can only delete your own messages");
        }
        message.setDeleted(true);
        chatMessageRepository.save(message);
        reactionService.deleteAllFor(ReactionTargetType.MESSAGE, messageId);
    }

    @Transactional
    public List<ReactionSummary> reactToMessage(Long messageId, String userEmail, String emoji) {
        ChatMessage message = findMessageOrThrow(messageId);
        User user = getUserOrThrow(userEmail);
        reactionService.react(ReactionTargetType.MESSAGE, messageId, user, emoji);
        return reactionService.getSummary(ReactionTargetType.MESSAGE, messageId);
    }

    private ChatMessage findMessageOrThrow(Long id) {
        return chatMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
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

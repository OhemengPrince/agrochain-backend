package com.agrochain.backend.service;

import com.agrochain.backend.dto.NotificationResponse;
import com.agrochain.backend.dto.NotificationsListResponse;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.Notification;
import com.agrochain.backend.model.NotificationType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.NotificationRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationsListResponse getNotifications(String userEmail) {
        User user = getUserOrThrow(userEmail);
        List<NotificationResponse> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationMapper::toResponse)
                .toList();
        long unreadCount = notificationRepository.findByUserAndIsRead(user, false).size();

        return NotificationsListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .build();
    }

    public void markAsRead(String userEmail, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You do not have access to this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public long markAllAsRead(String userEmail) {
        User user = getUserOrThrow(userEmail);
        List<Notification> unread = notificationRepository.findByUserAndIsRead(user, false);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return 0;
    }

    public void createNotification(Long userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

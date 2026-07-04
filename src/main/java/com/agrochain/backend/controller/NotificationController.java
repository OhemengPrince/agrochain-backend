package com.agrochain.backend.controller;

import com.agrochain.backend.dto.NotificationResponse;
import com.agrochain.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getNotifications(authentication.getName()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(Authentication authentication, @PathVariable Long id) {
        notificationService.markAsRead(authentication.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }
}

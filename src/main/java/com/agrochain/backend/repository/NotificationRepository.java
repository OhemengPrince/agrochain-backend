package com.agrochain.backend.repository;

import com.agrochain.backend.model.Notification;
import com.agrochain.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsRead(User user, boolean isRead);
}

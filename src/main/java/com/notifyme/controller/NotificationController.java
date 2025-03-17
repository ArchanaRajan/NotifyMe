package com.notifyme.controller;

import com.notifyme.dto.NotificationRequestDTO;
import com.notifyme.entity.NotificationRequest;
import com.notifyme.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<NotificationRequest> registerNotification(
            @Valid @RequestBody NotificationRequestDTO request) {
        log.info("Received notification registration request: {}", request);
        NotificationRequest notification = notificationService.registerNotification(request);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<NotificationRequest>> getNotifications(@PathVariable String email) {
        log.info("Fetching notifications for email: {}", email);
        List<NotificationRequest> notifications = notificationService.getNotificationsByEmail(email);
        return ResponseEntity.ok(notifications);
    }
}
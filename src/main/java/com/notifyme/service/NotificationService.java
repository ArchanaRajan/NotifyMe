package com.notifyme.service;

import com.notifyme.dto.NotificationRequestDTO;
import com.notifyme.entity.NotificationRequest;
import com.notifyme.entity.NotificationStatus;
import com.notifyme.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public NotificationRequest registerNotification(NotificationRequestDTO dto) {
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        NotificationRequest notification = new NotificationRequest();
        notification.setEmail(dto.getEmail());
        notification.setMovieName(dto.getMovieName());
        notification.setLocation(dto.getLocation());
        notification.setStartDate(dto.getStartDate());
        notification.setEndDate(dto.getEndDate());
        notification.setStatus(NotificationStatus.ACTIVE);

        notification = notificationRepository.save(notification);
        log.info("Registered new notification request: {}", notification);
        
        return notification;
    }

    public List<NotificationRequest> getNotificationsByEmail(String email) {
        return notificationRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    @Transactional
    public void processMovieRelease(String movieName, String location, LocalDate releaseDate) {
        List<NotificationRequest> activeNotifications = notificationRepository
            .findActiveNotificationsInDateRange(NotificationStatus.ACTIVE, releaseDate);

        for (NotificationRequest notification : activeNotifications) {
            if (notification.getMovieName().equalsIgnoreCase(movieName) &&
                notification.getLocation().equalsIgnoreCase(location)) {
                
                sendNotification(notification, releaseDate);
                notification.setStatus(NotificationStatus.NOTIFIED);
                notificationRepository.save(notification);
            }
        }
    }

    private void sendNotification(NotificationRequest notification, LocalDate releaseDate) {
        String subject = String.format("Movie Alert: %s is now available!", notification.getMovieName());
        String body = String.format(
            "Dear Movie Fan,\n\n" +
            "Great news! The movie '%s' is now available for booking in %s on %s.\n\n" +
            "Don't miss out - book your tickets now!\n\n" +
            "Best regards,\nNotifyMe Team",
            notification.getMovieName(),
            notification.getLocation(),
            releaseDate
        );

        emailService.sendEmail(notification.getEmail(), subject, body);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }

    @Transactional
    public void cleanupExpiredNotifications() {
        List<NotificationRequest> expiredNotifications = notificationRepository
            .findExpiredNotifications(NotificationStatus.ACTIVE, LocalDate.now());

        for (NotificationRequest notification : expiredNotifications) {
            notification.setStatus(NotificationStatus.EXPIRED);
            notificationRepository.save(notification);
            log.info("Marked notification as expired: {}", notification);
        }
    }
} 
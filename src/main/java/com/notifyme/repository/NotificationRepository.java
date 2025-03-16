package com.notifyme.repository;

import com.notifyme.entity.NotificationRequest;
import com.notifyme.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationRequest, Long> {
    
    List<NotificationRequest> findByEmailOrderByCreatedAtDesc(String email);
    
    List<NotificationRequest> findByStatus(NotificationStatus status);
    
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = :status " +
           "AND n.startDate <= :currentDate AND n.endDate >= :currentDate")
    List<NotificationRequest> findActiveNotificationsInDateRange(
            @Param("status") NotificationStatus status,
            @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT n FROM NotificationRequest n WHERE n.status = :status " +
           "AND n.endDate < :currentDate")
    List<NotificationRequest> findExpiredNotifications(
            @Param("status") NotificationStatus status,
            @Param("currentDate") LocalDate currentDate);
} 
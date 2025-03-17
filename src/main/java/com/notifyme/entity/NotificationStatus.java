package com.notifyme.entity;

public enum NotificationStatus {
    ACTIVE,      // Notification is active and waiting for movie release
    NOTIFIED,    // User has been notified about the movie release
    EXPIRED,     // Notification request has expired (past end date)
    CANCELLED    // User has cancelled the notification request
} 
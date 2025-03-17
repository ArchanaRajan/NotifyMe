package com.notifyme.scheduler;

import com.notifyme.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailRateLimitScheduler {

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 * * * *") // Run at the start of every hour
    public void resetEmailRateLimit() {
        log.info("Resetting email rate limit counter");
        emailService.resetHourlyCount();
    }
} 
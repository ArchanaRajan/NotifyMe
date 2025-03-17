package com.notifyme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.rate.limit.per.hour:50}")
    private int hourlyLimit;

    private final AtomicInteger hourlyCount = new AtomicInteger(0);

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendEmail(String to, String subject, String body) {
        if (hourlyCount.get() >= hourlyLimit) {
            log.warn("Hourly email limit reached. Email to {} queued for next hour.", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            hourlyCount.incrementAndGet();
            
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw e;
        }
    }

    // Reset hourly count (should be called by a scheduled task)
    public void resetHourlyCount() {
        hourlyCount.set(0);
    }
} 
package com.notifyme.controller;

import com.notifyme.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/simulate-release")
    public ResponseEntity<String> simulateMovieRelease(
            @RequestParam String movieName,
            @RequestParam String location,
            @RequestParam(required = false) String releaseDate) {
        
        LocalDate date = releaseDate != null ? LocalDate.parse(releaseDate) : LocalDate.now();
        log.info("Simulating release for movie: {} in {} on {}", movieName, location, date);
        
        notificationService.processMovieRelease(movieName, location, date);
        return ResponseEntity.ok("Movie release simulation completed");
    }
} 
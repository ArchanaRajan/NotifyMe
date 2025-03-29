package com.notifyme.controller;

import com.notifyme.dto.MovieShow;
import com.notifyme.service.NotificationService;
import com.notifyme.service.scraper.BookMyShowScraperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookMyShowScraperService bookMyShowScraperService;

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

    @PostMapping("/scrape")
    public ResponseEntity<List<MovieShow>> testScraping(
            @RequestParam String movieName,
            @RequestParam String location) {
        log.info("Manual scraping triggered for movie: {} in location: {}", movieName, location);
        List<MovieShow> shows = bookMyShowScraperService.scrapeMovieShows(movieName, location);
        return ResponseEntity.ok(shows);
    }
} 
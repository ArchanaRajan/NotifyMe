package com.notifyme.controller;

import com.notifyme.model.MovieShow;
import com.notifyme.service.ScrapingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {

    private final ScrapingService scrapingService;

    @Autowired
    public ScrapingController(ScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    @GetMapping("/bookmyshow")
    public ResponseEntity<List<MovieShow>> scrapeBookMyShowMovies(
            @RequestParam String movieName,
            @RequestParam String location) {
        try {
            List<MovieShow> shows = scrapingService.scrapeBookMyShowMovies(movieName, location);
            return ResponseEntity.ok(shows);
        } catch (Exception e) {
            log.error("Error scraping movies for movie: {} in location: {}", movieName, location, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
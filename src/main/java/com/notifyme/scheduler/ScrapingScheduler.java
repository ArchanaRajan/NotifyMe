package com.notifyme.scheduler;

import com.notifyme.service.scraper.BookMyShowScraperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ScrapingScheduler {

    @Autowired
    private BookMyShowScraperService bookMyShowScraperService;

    // List of movies to monitor
    private final List<String> moviesToMonitor = Arrays.asList(
        "Interstellar",
        "Snow White");

    // List of locations to monitor
    private final List<String> locationsToMonitor = Arrays.asList("Bangalore", "Chennai");

    @Scheduled(cron = "0 */3 * * * *") // Run every 3 minutes
    public void scrapeBookMyShow() {
        log.info("Starting BookMyShow scraping job");
        for (String movie : moviesToMonitor) {
            for (String location : locationsToMonitor) {
                try {
                    bookMyShowScraperService.scrapeMovieShows(movie, location);
                    // Add delay between requests to avoid rate limiting
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.error("Error scraping {} in {}: {}", movie, location, e.getMessage());
                }
            }
        }
        log.info("Completed BookMyShow scraping job");
    }
} 
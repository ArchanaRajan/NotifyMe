package com.notifyme.scheduler;

import com.notifyme.scraper.PVRScraper;
import com.notifyme.scraper.BookMyShowScraper;
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
    private PVRScraper pvrScraper;

    @Autowired
    private BookMyShowScraper bookMyShowScraper;

    // List of movies to monitor
    private final List<String> moviesToMonitor = Arrays.asList(
        "BLACK BAG");

    // List of locations to monitor
    private final List<String> locationsToMonitor = Arrays.asList("Chennai");

    @Scheduled(cron = "0 */3 * * * *") // Run every 3 minutes
    public void scrapeMovieShows() {
        log.info("Starting movie show scraping job");
        
        for (String movie : moviesToMonitor) {
            for (String location : locationsToMonitor) {
                try {
                    // Scrape PVR
                    log.info("Scraping PVR for movie: {} in {}", movie, location);
                    pvrScraper.scrapeMovieShows(movie, location);
                    Thread.sleep(5000); // Delay between scrapers

                    // Scrape BookMyShow
                    log.info("Scraping BookMyShow for movie: {} in {}", movie, location);
                    bookMyShowScraper.scrapeMovieShows(movie, location);
                    Thread.sleep(5000); // Delay between movies
                } catch (Exception e) {
                    log.error("Error scraping {} in {}: {}", movie, location, e.getMessage());
                }
            }
        }
        log.info("Completed movie show scraping job");
    }
} 
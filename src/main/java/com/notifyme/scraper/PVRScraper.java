package com.notifyme.scraper;

import com.notifyme.dto.MovieShow;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PVRScraper extends BaseScraper {

    private static final String BASE_URL = "https://www.pvrcinemas.com";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private static final By MOVIE_CARD = By.cssSelector(".movie-card");
    private static final By MOVIE_NAME = By.cssSelector(".movie-name");
    private static final By THEATER_NAME = By.cssSelector(".theater-name");
    private static final By SHOW_TIME = By.cssSelector(".show-time");
    private static final By PRICE_RANGE = By.cssSelector(".price-range");
    private static final By BOOKING_LINK = By.cssSelector("a");

    @Value("${scraping.pvr.user-agent}")
    private String userAgent;

    @Value("${scraping.pvr.delay-between-requests:5000}")
    private long delayBetweenRequests;

    public PVRScraper(WebDriver webDriver) {
        super(webDriver);
    }

    public List<MovieShow> scrapeMovieShows(String movieName, String location) {
        List<MovieShow> shows = new ArrayList<>();
        try {
            log.info("Scraping PVR shows for movie: {} in location: {}", movieName, location);
            navigateTo(BASE_URL);

            // Wait for movie cards to load
            List<WebElement> movieCards = findElements(MOVIE_CARD);
            log.info("Found {} movie cards", movieCards.size());

            for (WebElement card : movieCards) {
                try {
                    Optional<WebElement> nameElement = findElement(MOVIE_NAME);
                    Optional<WebElement> theaterElement = findElement(THEATER_NAME);
                    Optional<WebElement> showTimeElement = findElement(SHOW_TIME);
                    Optional<WebElement> priceElement = findElement(PRICE_RANGE);
                    Optional<WebElement> bookingLinkElement = findElement(BOOKING_LINK);

                    if (nameElement.isPresent() && theaterElement.isPresent() && 
                        showTimeElement.isPresent() && priceElement.isPresent() && 
                        bookingLinkElement.isPresent()) {
                        
                        String showTime = showTimeElement.get().getText();
                        String priceText = priceElement.get().getText().replaceAll("[^0-9.]", "");
                        double price = Double.parseDouble(priceText);
                        
                        MovieShow show = new MovieShow(
                            nameElement.get().getText(),
                            location,
                            theaterElement.get().getText(),
                            LocalDate.now(),
                            showTime,
                            bookingLinkElement.get().getAttribute("href"),
                            price
                        );
                        
                        shows.add(show);
                        log.info("Found show: {}", show);
                    }
                } catch (Exception e) {
                    log.error("Error processing movie card: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error scraping PVR shows: {}", e.getMessage());
        }
        return shows;
    }
}
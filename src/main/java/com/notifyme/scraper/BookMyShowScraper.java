package com.notifyme.scraper;

import com.notifyme.dto.MovieShow;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class BookMyShowScraper extends BaseScraper {

    private static final String BASE_URL = "https://in.bookmyshow.com";
    private static final String SEARCH_URL = BASE_URL + "/search?q=%s";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private static final By MOVIE_CARD = By.cssSelector(".movie-card");
    private static final By MOVIE_NAME = By.cssSelector(".movie-name");
    private static final By THEATER_NAME = By.cssSelector(".theater-name");
    private static final By SHOW_TIME = By.cssSelector(".show-time");
    private static final By PRICE_RANGE = By.cssSelector(".price-range");
    private static final By BOOKING_LINK = By.cssSelector("a");

    @Value("${scraping.bookmyshow.user-agent}")
    private String userAgent;

    @Value("${scraping.bookmyshow.delay-between-requests:5000}")
    private long delayBetweenRequests;

    public BookMyShowScraper(WebDriver webDriver) {
        super(webDriver);
    }

    public List<MovieShow> scrapeMovieShows(String movieName, String location) {
        List<MovieShow> shows = new ArrayList<>();
        try {
            String searchUrl = String.format(SEARCH_URL, movieName.replace(" ", "+"));
            log.info("Accessing URL: {}", searchUrl);
            
            navigateTo(searchUrl);
            
            // Wait for the movie cards to load
            waitForElementToBeClickable(MOVIE_CARD);
            
            // Add delay to avoid rate limiting
            Thread.sleep(delayBetweenRequests);
            
            List<WebElement> movieElements = findElements(MOVIE_CARD);
            log.info("Found {} movie elements", movieElements.size());
            
            for (WebElement movieElement : movieElements) {
                try {
                    Optional<MovieShow> show = parseMovieElement(movieElement, location);
                    show.ifPresent(shows::add);
                } catch (Exception e) {
                    log.error("Error parsing movie element: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error scraping BookMyShow: {}", e.getMessage());
        }
        return shows;
    }

    private Optional<MovieShow> parseMovieElement(WebElement element, String location) {
        try {
            String movieName = element.findElement(MOVIE_NAME).getText();
            String theaterName = element.findElement(THEATER_NAME).getText();
            String showTimeStr = element.findElement(SHOW_TIME).getText();
            String priceRange = element.findElement(PRICE_RANGE).getText();
            String bookingUrl = element.findElement(BOOKING_LINK).getAttribute("href");

            LocalDateTime showTime = parseShowTime(showTimeStr);

            return Optional.of(MovieShow.builder()
                    .movieName(movieName)
                    .theaterName(theaterName)
                    .location(location)
                    .showTime(showTime)
                    .priceRange(priceRange)
                    .bookingUrl(bookingUrl)
                    .source("bookmyshow")
                    .isAvailable(true)
                    .scrapedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Error parsing movie element: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private LocalDateTime parseShowTime(String showTimeStr) {
        try {
            // Example format: "Today, 2:30 PM"
            String[] parts = showTimeStr.split(", ");
            String timeStr = parts[1];
            LocalDateTime now = LocalDateTime.now();
            
            // Parse time
            String[] timeParts = timeStr.split(" ");
            String[] hourMin = timeParts[0].split(":");
            int hour = Integer.parseInt(hourMin[0]);
            int minute = Integer.parseInt(hourMin[1]);
            boolean isPM = timeParts[1].equals("PM");
            
            // Adjust hour for PM
            if (isPM && hour != 12) {
                hour += 12;
            } else if (!isPM && hour == 12) {
                hour = 0;
            }
            
            return now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        } catch (Exception e) {
            log.error("Error parsing show time: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    public String getPlatformName() {
        return "bookmyshow";
    }
}
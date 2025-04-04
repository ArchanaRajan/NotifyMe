package com.notifyme.scraper;

import com.notifyme.dto.MovieShow;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
public class BookMyShowScraper extends BaseScraper {

    // Base URL and location-specific URL
    private static final String BASE_URL = "https://in.bookmyshow.com";
    private static final String LANDING_URL = BASE_URL + "/explore/home/%s";
    
    // Selectors for Movie Listing Page (Landing Page)
    private static final By MOVIE_CARD_LISTING = By.cssSelector("div a[href*='/movies/']");
    private static final By MOVIE_TITLE_LISTING = By.cssSelector(".sc-7o7nez-0.daKrZU");
    private static final By MOVIE_IMAGE_LISTING = By.cssSelector("img[alt]");
    
    // Selectors for Movie Details Page
    private static final By BOOK_TICKETS_BUTTON = By.cssSelector("button[data-phase='postRelease']");
    
    // Selectors for Showtimes Page
    private static final By THEATRE_CONTAINER_SESSION = By.cssSelector(".venue-name");
    private static final By THEATRE_NAME_SESSION = By.cssSelector(".venue-name");
    private static final By SHOWTIME_CONTAINER_SESSION = By.cssSelector(".showtime-pill");
    private static final By SHOW_TIME_SESSION = By.cssSelector(".time");
    private static final By PRICE_RANGE_SESSION = By.cssSelector(".price");
    private static final By BOOKING_LINK_SESSION = By.cssSelector("a");
    
    // Cloudflare detection selectors
    private static final By CLOUDFLARE_CHALLENGE = By.cssSelector("#challenge-form, #cf-content, .cf-browser-verification");
    private static final Duration CLOUDFLARE_TIMEOUT = Duration.ofSeconds(30);

    @Value("${scraping.bookmyshow.delay-between-requests:2000}")
    private long delayBetweenRequests;

    private final Random random = new Random();

    public BookMyShowScraper(WebDriver webDriver) {
        super(webDriver);
    }

    public List<MovieShow> scrapeMovieShows(String movieName, String location) {
        List<MovieShow> shows = new ArrayList<>();
        log.info("Starting BookMyShow scrape for movie: '{}' in location: '{}'", movieName, location);

        try {
            // 1. Navigate to the location-specific landing page
            String landingUrl = String.format(LANDING_URL, location.toLowerCase());
            log.info("Navigating to BookMyShow landing page: {}", landingUrl);
            navigateTo(landingUrl);
            
            // Check for Cloudflare challenge
            if (isCloudflareChallenge()) {
                log.info("Detected Cloudflare challenge on landing page, waiting for resolution...");
                waitForCloudflareChallenge();
            }
            
            // Add random delay to simulate human behavior
            Thread.sleep(delayBetweenRequests + random.nextInt(1000));

            // 2. Find the specific movie card
            log.info("Searching for movie card for '{}'", movieName);
            List<WebElement> movieCards = findElements(MOVIE_CARD_LISTING);
            log.info("Found {} movie cards on the landing page", movieCards.size());
            
            Optional<WebElement> targetMovieCard = movieCards.stream()
                .filter(card -> {
                    Optional<WebElement> titleElement = findElementWithin(card, MOVIE_TITLE_LISTING);
                    Optional<WebElement> imageElement = findElementWithin(card, MOVIE_IMAGE_LISTING);
                    
                    String title = titleElement.map(WebElement::getText).orElse("");
                    String altText = imageElement.map(e -> e.getAttribute("alt")).orElse("");
                    
                    log.debug("Checking movie: Title='{}', Alt='{}'", title, altText);
                    
                    return title.equalsIgnoreCase(movieName) || altText.equalsIgnoreCase(movieName);
                })
                .findFirst();

            if (targetMovieCard.isEmpty()) {
                log.info("Movie '{}' not found on the landing page.", movieName);
                return shows;
            }
            log.info("Found movie card for '{}'", movieName);

            // 3. Click on the movie card to navigate to the movie details page
            log.info("Clicking on movie card to navigate to details page...");
            try {
                // 1. Scroll the card into view using JavaScript
                ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", targetMovieCard.get());
                Thread.sleep(500 + random.nextInt(500)); // Random pause after scrolling

                // 2. Wait explicitly for the element to be clickable
                WebElement clickableCard = wait.until(ExpectedConditions.elementToBeClickable(targetMovieCard.get()));
                
                // 3. Perform the click
                clickableCard.click();
                log.info("Successfully clicked on movie card for '{}'", movieName);

            } catch (Exception e) {
                log.error("Failed to click on the movie card for '{}'. Error: {}. Trying JavaScript click as fallback.", movieName, e.getMessage());
                // Fallback: Try clicking using JavaScript if the standard click failed
                try {
                   ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", targetMovieCard.get());
                   log.info("Successfully clicked on movie card via JavaScript fallback.");
                } catch (Exception jsException) {
                    log.error("JavaScript fallback click also failed for movie '{}': {}", movieName, jsException.getMessage());
                    return shows;
                }
            }

            // 4. Wait for the movie details page to load
            log.info("Waiting for movie details page to load...");
            Thread.sleep(5000);
            
            // Check for Cloudflare challenge on movie details page
            if (isCloudflareChallenge()) {
                log.info("Detected Cloudflare challenge on movie details page, waiting for resolution...");
                waitForCloudflareChallenge();
            }
            
            // 5. Find and click the 'Book tickets' button
            log.info("Looking for 'Book tickets' button...");
            Optional<WebElement> bookTicketsButton = findElement(BOOK_TICKETS_BUTTON);
            
            if (bookTicketsButton.isEmpty()) {
                log.error("Could not find 'Book tickets' button for movie '{}'", movieName);
                return shows;
            }
            
            log.info("Attempting to click 'Book tickets' button...");
            try {
                // 1. Scroll the button into view using JavaScript
                ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", bookTicketsButton.get());
                Thread.sleep(500 + random.nextInt(500)); // Random pause after scrolling

                // 2. Wait explicitly for the element to be clickable
                WebElement clickableButton = wait.until(ExpectedConditions.elementToBeClickable(bookTicketsButton.get()));
                
                // 3. Perform the click
                clickableButton.click();
                log.info("Successfully clicked 'Book tickets' button for '{}'", movieName);

            } catch (Exception e) {
                log.error("Failed to click the 'Book tickets' button for movie '{}'. Error: {}. Trying JavaScript click as fallback.", movieName, e.getMessage());
                // Fallback: Try clicking using JavaScript if the standard click failed
                try {
                   ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", bookTicketsButton.get());
                   log.info("Successfully clicked 'Book tickets' button via JavaScript fallback.");
                } catch (Exception jsException) {
                    log.error("JavaScript fallback click also failed for movie '{}': {}", movieName, jsException.getMessage());
                    return shows;
                }
            }

            // 6. Wait for the showtimes page to load
            log.info("Waiting for showtimes page to load...");
            Thread.sleep(5000);
            
            // Check for Cloudflare challenge on showtimes page
            if (isCloudflareChallenge()) {
                log.info("Detected Cloudflare challenge on showtimes page, waiting for resolution...");
                waitForCloudflareChallenge();
            }
            
            // 7. Scrape showtimes from the showtimes page
            List<WebElement> theatreContainers = findElements(THEATRE_CONTAINER_SESSION);
            log.info("Found {} potential theatre containers on showtimes page.", theatreContainers.size());

            for (WebElement theatreContainer : theatreContainers) {
                String theatreName = "Unknown Theatre";
                try {
                    // Extract Theatre Name
                    Optional<WebElement> theatreNameElement = findElementWithin(theatreContainer, THEATRE_NAME_SESSION);
                    if (theatreNameElement.isPresent()) {
                        theatreName = theatreNameElement.get().getText().trim();
                        log.info("Processing theatre: {}", theatreName);
                    } else {
                        log.warn("Could not extract theatre name from a container.");
                        continue;
                    }

                    // Find showtime boxes within the theatre container
                    List<WebElement> showtimeBoxes = findElementsWithin(theatreContainer, SHOWTIME_CONTAINER_SESSION);
                    log.info("Found {} showtime boxes for theatre '{}'", showtimeBoxes.size(), theatreName);

                    for (WebElement showtimeBox : showtimeBoxes) {
                        try {
                            Optional<WebElement> timeElement = findElementWithin(showtimeBox, SHOW_TIME_SESSION);
                            Optional<WebElement> priceElement = findElementWithin(showtimeBox, PRICE_RANGE_SESSION);
                            Optional<WebElement> linkElement = findElementWithin(showtimeBox, BOOKING_LINK_SESSION);

                            if (timeElement.isPresent()) {
                                String showTime = timeElement.get().getText().trim();
                                String priceRange = priceElement.map(WebElement::getText).orElse("N/A");
                                String bookingUrl = linkElement.map(e -> e.getAttribute("href")).orElse(webDriver.getCurrentUrl());
                                
                                // Create MovieShow DTO using Builder
                                MovieShow show = MovieShow.builder()
                                    .movieName(movieName)
                                    .location(location)
                                    .theaterName(theatreName)
                                    .showTime(LocalDateTime.now()) // Placeholder: Ideally parse showTime string
                                    .priceRange(priceRange)
                                    .bookingUrl(bookingUrl)
                                    .source("BookMyShow")
                                    .isAvailable(true)
                                    .scrapedAt(LocalDateTime.now())
                                    .build();
                                    
                                shows.add(show);
                                log.info("Found show: Movie='{}', Theatre='{}', Time='{}', Price='{}'", 
                                    movieName, theatreName, showTime, priceRange);
                            } else {
                                log.warn("Could not find time element within a showtime box for theatre '{}'", theatreName);
                            }
                        } catch (Exception e) {
                            log.error("Error processing a showtime box for theatre '{}': {}", theatreName, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing theatre container: {}", e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Major error during BookMyShow scraping for movie '{}': {}", movieName, e.getMessage(), e);
        } finally {
            log.info("BookMyShow scraping finished for movie '{}'. Found {} shows.", movieName, shows.size());
        }
        return shows;
    }
    
    /**
     * Checks if the current page contains a Cloudflare challenge
     */
    private boolean isCloudflareChallenge() {
        try {
            // Check for common Cloudflare challenge elements
            return findElement(CLOUDFLARE_CHALLENGE).isPresent() || 
                   webDriver.getPageSource().contains("challenge-platform") ||
                   webDriver.getPageSource().contains("cf-browser-verification") ||
                   webDriver.getPageSource().contains("Cloudflare");
        } catch (Exception e) {
            log.warn("Error checking for Cloudflare challenge: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Waits for the Cloudflare challenge to be resolved
     */
    private void waitForCloudflareChallenge() {
        try {
            log.info("Waiting for Cloudflare challenge to be resolved (timeout: {} seconds)...", CLOUDFLARE_TIMEOUT.getSeconds());
            
            // Wait for the challenge to disappear
            long startTime = System.currentTimeMillis();
            while (isCloudflareChallenge() && System.currentTimeMillis() - startTime < CLOUDFLARE_TIMEOUT.toMillis()) {
                log.info("Cloudflare challenge still present, waiting...");
                Thread.sleep(2000);
                
                // Try to interact with the page to help resolve the challenge
                try {
                    // Scroll a bit to simulate human behavior
                    ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, 100);");
                    Thread.sleep(500);
                    ((JavascriptExecutor) webDriver).executeScript("window.scrollBy(0, -100);");
                } catch (Exception e) {
                    // Ignore any errors during interaction
                }
            }
            
            if (isCloudflareChallenge()) {
                log.warn("Cloudflare challenge not resolved within timeout period");
            } else {
                log.info("Cloudflare challenge resolved successfully");
            }
            
            // Add a delay after challenge resolution
            Thread.sleep(3000);
        } catch (Exception e) {
            log.error("Error waiting for Cloudflare challenge: {}", e.getMessage());
        }
    }
}
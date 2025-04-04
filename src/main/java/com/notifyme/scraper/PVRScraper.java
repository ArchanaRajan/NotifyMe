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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PVRScraper extends BaseScraper {

    // Selectors for Movie Listing Page
    private static final String LISTING_BASE_URL = "https://www.pvrcinemas.com/"; // Adjust if location selection is needed first
    private static final By MOVIE_CARD_LISTING = By.cssSelector(".p-card");
    private static final By MOVIE_TITLE_LISTING = By.cssSelector(".p-card-title span");
    private static final By BOOK_BUTTON_LISTING = By.cssSelector(".book-tickets-btn");

    // Selectors for Session/Showtime Page
    private static final By SESSION_LOAD_INDICATOR = By.cssSelector(".p-accordion"); // Container for theatre list
    private static final By THEATRE_CONTAINER_SESSION = By.cssSelector(".p-accordion-tab"); // Each theatre block
    private static final By THEATRE_NAME_SESSION = By.cssSelector(".cinema-listed-locat h2"); // Theatre name within block header
    private static final By SHOWTIME_CONTAINER_SESSION = By.cssSelector(".box-slot-moviesession"); // Each showtime box within block content
    private static final By SHOW_TIME_SESSION = By.cssSelector(".show-times h5"); // The actual time text
    private static final By ACCORDION_HEADER_LINK = By.cssSelector(".p-accordion-header-link"); // Link to expand theatre
    private static final By ACCORDION_CONTENT = By.cssSelector(".p-accordion-content"); // Content div shown after expanding

    @Value("${scraping.pvr.delay-between-requests:1000}") // Reduced default delay, adjust as needed
    private long delayBetweenRequests;

    public PVRScraper(WebDriver webDriver) {
        super(webDriver);
    }

    public List<MovieShow> scrapeMovieShows(String movieName, String location) {
        List<MovieShow> shows = new ArrayList<>();
        log.info("Starting PVR scrape for movie: '{}' in location: '{}'", movieName, location);

        try {
            // 1. Navigate to the main listing page
            // Note: Location selection might be needed here if not part of URL or default
            log.info("Navigating to PVR listing page: {}", LISTING_BASE_URL);
            navigateTo(LISTING_BASE_URL);
            // Optional: Implement location selection if needed (e.g., clicking a city dropdown)
            // waitForElementToBeClickable(By.id("city-selector")).ifPresent(WebElement::click);
            // findElement(By.xpath(String.format("//li[contains(text(),'%s')]/parent::div", location))).ifPresent(WebElement::click);
            // Thread.sleep(delayBetweenRequests); // Wait after location selection

            // 2. Find the specific movie card
            log.info("Searching for movie card for '{}'", movieName);
            List<WebElement> movieCards = findElements(MOVIE_CARD_LISTING);
            Optional<WebElement> targetMovieCard = movieCards.stream()
                .filter(card -> {
                    Optional<WebElement> titleElement = findElementWithin(card, MOVIE_TITLE_LISTING);
                    return titleElement.map(WebElement::getText).orElse("").equalsIgnoreCase(movieName);
                })
                .findFirst();

            if (targetMovieCard.isEmpty()) {
                log.info("Movie '{}' not found on the listing page.", movieName);
                return shows;
            }
            log.info("Found movie card for '{}'", movieName);

            // 3. Find and click the 'Book' button within that card
            Optional<WebElement> bookButton = findElementWithin(targetMovieCard.get(), BOOK_BUTTON_LISTING);
            if (bookButton.isEmpty()) {
                log.error("Could not find 'Book' button for movie '{}'", movieName);
                return shows;
            }

            log.info("Attempting to click 'Book' button...");
            try {
                // 1. Scroll the button into view using JavaScript
                ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", bookButton.get());
                Thread.sleep(500); // Short pause after scrolling 

                // 2. Wait explicitly for the element to be clickable
                WebElement clickableButton = wait.until(ExpectedConditions.elementToBeClickable(bookButton.get()));
                
                // 3. Perform the click
                clickableButton.click();
                log.info("Successfully clicked 'Book' button for '{}'", movieName);

            } catch (Exception e) {
                log.error("Failed to click the 'Book' button for movie '{}'. Error: {}. Trying JavaScript click as fallback.", movieName, e.getMessage());
                // Fallback: Try clicking using JavaScript if the standard click failed
                try {
                   ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", bookButton.get());
                   log.info("Successfully clicked 'Book' button via JavaScript fallback.");
                } catch (Exception jsException) {
                    log.error("JavaScript fallback click also failed for movie '{}': {}", movieName, jsException.getMessage());
                    // If both fail, we probably can't proceed for this movie
                    return shows; 
                }
            }

            // 4. Wait for the session page to load
            log.info("Waiting for session page content to load...");
            Thread.sleep(5000);
            wait.until(ExpectedConditions.presenceOfElementLocated(SESSION_LOAD_INDICATOR));
            Thread.sleep(delayBetweenRequests); // Allow dynamic content to potentially settle
            log.info("Session page loaded.");

            // 5. Scrape showtimes from the session page
            List<WebElement> theatreContainers = findElements(THEATRE_CONTAINER_SESSION);
            log.info("Found {} potential theatre containers on session page.", theatreContainers.size());

            for (WebElement theatreContainer : theatreContainers) {
                String theatreName = "Unknown Theatre";
                try {
                    // Extract Theatre Name from header
                    Optional<WebElement> theatreNameElement = findElementWithin(theatreContainer, THEATRE_NAME_SESSION);
                    if (theatreNameElement.isPresent()) {
                        theatreName = theatreNameElement.get().getText().trim();
                        log.info("Processing theatre: {}", theatreName);
                    } else {
                        log.warn("Could not extract theatre name from a container.");
                        continue; // Skip if name isn't found
                    }

                    // Check if the theatre section needs expanding (check aria-expanded on header link)
                    Optional<WebElement> headerLink = findElementWithin(theatreContainer, ACCORDION_HEADER_LINK);
                    boolean isExpanded = headerLink.map(link -> "true".equalsIgnoreCase(link.getAttribute("aria-expanded"))).orElse(false);
                    
                    // PVR seems to load the first one expanded, others need click
                    // Check if content div is present, if not, click header to expand
                    Optional<WebElement> contentDiv = findElementWithin(theatreContainer, ACCORDION_CONTENT, 1); // Short timeout
                    if (contentDiv.isEmpty()) {
                       log.info("Theatre '{}' content not visible, attempting to expand.", theatreName);
                       headerLink.ifPresent(WebElement::click);
                       Thread.sleep(delayBetweenRequests / 2); // Wait for expansion animation/load
                       // Re-find content after potential click
                       contentDiv = findElementWithin(theatreContainer, ACCORDION_CONTENT, 5); // Longer timeout after click
                    }

                    if (contentDiv.isEmpty()) {
                        log.warn("Could not find or expand content for theatre '{}'", theatreName);
                        continue;
                    }
                    
                    // Find showtime boxes within the *now visible* content
                    List<WebElement> showtimeBoxes = findElementsWithin(contentDiv.get(), SHOWTIME_CONTAINER_SESSION);
                    log.info("Found {} showtime boxes for theatre '{}'", showtimeBoxes.size(), theatreName);

                    for (WebElement showtimeBox : showtimeBoxes) {
                        try {
                            Optional<WebElement> timeElement = findElementWithin(showtimeBox, SHOW_TIME_SESSION);
                            if (timeElement.isPresent()) {
                                String showTime = timeElement.get().getText().trim();
                                
                                // Create MovieShow DTO using Builder
                                MovieShow show = MovieShow.builder()
                                    .movieName(movieName)
                                    .location(location) // Assuming location passed is correct
                                    .theaterName(theatreName)
                                    .showTime(LocalDateTime.now()) // Placeholder: Ideally parse showTime string
                                    .priceRange("N/A") // Price not available from this view
                                    .bookingUrl(webDriver.getCurrentUrl()) // Use session page URL as placeholder
                                    .source("PVR") // Set correct source
                                    .isAvailable(true) // Assume available if time is listed
                                    .scrapedAt(LocalDateTime.now())
                                    .build();
                                    
                                shows.add(show);
                                log.info("Found show: Movie='{}', Theatre='{}', Time='{}'", movieName, theatreName, showTime);
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
            log.error("Major error during PVR scraping for movie '{}': {}", movieName, e.getMessage(), e);
        } finally {
            log.info("PVR scraping finished for movie '{}'. Found {} shows.", movieName, shows.size());
            // Consider closing the browser tab or navigating away if needed, but usually BaseScraper handles quit
        }
        return shows;
    }
}
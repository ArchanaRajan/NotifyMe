package com.notifyme.scraper;

import com.notifyme.dto.MovieShow;
import com.notifyme.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private static final By SHOW_LANGUAGE = By.cssSelector(".eng h6"); // Language of the show

    @Value("${scraping.pvr.delay-between-requests:1000}") // Reduced default delay, adjust as needed
    private long delayBetweenRequests;
    
    @Value("${spring.mail.username}")
    private String senderEmail;
    
    @Value("${notification.email:archana19rajan@gmail.com}")
    private String recipientEmail;
    
    @Value("${scraping.preferred.pvr.theatre:PVR Theyagaraja Thiruvanmiyur Chennai}")
    private String preferredTheatre;
    
    @Autowired
    private EmailService emailService;

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
            
            // Flag to track if preferred theatre was found
            boolean preferredTheatreFound = false;
            List<String> showTimings = new ArrayList<>();

            for (WebElement theatreContainer : theatreContainers) {
                String theatreName = "Unknown Theatre";
                try {
                    // Extract Theatre Name from header
                    Optional<WebElement> theatreNameElement = findElementWithin(theatreContainer, THEATRE_NAME_SESSION);
                    if (theatreNameElement.isPresent()) {
                        theatreName = theatreNameElement.get().getText().trim();
                        log.info("Processing theatre: {}", theatreName);
                        
                        // Check if this is the preferred theatre
                        if (theatreName.contains(preferredTheatre)) {
                            preferredTheatreFound = true;
                            log.info("Found preferred theatre: {}", theatreName);
                        }
                    } else {
                        log.warn("Could not extract theatre name from a container.");
                        continue; // Skip if name isn't found
                    }

                    // Check if the theatre section is already expanded
                    // In the provided HTML, the active tab has class "p-accordion-tab-active"
                    boolean isAlreadyExpanded = theatreContainer.getAttribute("class").contains("p-accordion-tab-active");
                    
                    // Find the content div - it should be visible if the section is already expanded
                    Optional<WebElement> contentDiv = findElementWithin(theatreContainer, ACCORDION_CONTENT, 1); // Short timeout
                    
                    // If content is not visible and section is not already expanded, try to expand it
                    if (contentDiv.isEmpty() && !isAlreadyExpanded) {
                        log.info("Theatre '{}' content not visible, attempting to expand.", theatreName);
                        try {
                            // Try JavaScript click instead of direct click to avoid the error
                            Optional<WebElement> headerLink = findElementWithin(theatreContainer, ACCORDION_HEADER_LINK);
                            if (headerLink.isPresent()) {
                                ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", headerLink.get());
                                log.info("Successfully clicked header link via JavaScript for theatre '{}'", theatreName);
                            }
                        } catch (Exception e) {
                            log.error("Failed to expand theatre '{}' section: {}", theatreName, e.getMessage());
                        }
                        Thread.sleep(delayBetweenRequests / 2); // Wait for expansion animation/load
                        // Re-find content after potential click
                        contentDiv = findElementWithin(theatreContainer, ACCORDION_CONTENT, 5); // Longer timeout after click
                    }

                    if (contentDiv.isEmpty()) {
                        log.warn("Could not find or expand content for theatre '{}'", theatreName);
                        continue;
                    }
                    
                    // Find showtime boxes within the content
                    List<WebElement> showtimeBoxes = findElementsWithin(contentDiv.get(), SHOWTIME_CONTAINER_SESSION);
                    log.info("Found {} showtime boxes for theatre '{}'", showtimeBoxes.size(), theatreName);
                    
                    // If this is the preferred theatre, collect all show timings
                    if (theatreName.contains(preferredTheatre)) {
                        for (WebElement showtimeBox : showtimeBoxes) {
                            Optional<WebElement> timeElement = findElementWithin(showtimeBox, SHOW_TIME_SESSION);
                            Optional<WebElement> languageElement = findElementWithin(showtimeBox, SHOW_LANGUAGE);
                            
                            if (timeElement.isPresent()) {
                                String showTime = timeElement.get().getText().trim();
                                String language = languageElement.map(WebElement::getText).map(String::trim).orElse("");
                                
                                // Format the show time with language if available
                                String formattedTime = language.isEmpty() ? showTime : showTime + " (" + language + ")";
                                showTimings.add(formattedTime);
                                log.info("Added show timing: {}", formattedTime);
                            }
                        }
                        log.info("Found {} show timings for preferred theatre: {}", showTimings.size(), showTimings);
                    }

                    for (WebElement showtimeBox : showtimeBoxes) {
                        try {
                            Optional<WebElement> timeElement = findElementWithin(showtimeBox, SHOW_TIME_SESSION);
                            Optional<WebElement> languageElement = findElementWithin(showtimeBox, SHOW_LANGUAGE);
                            
                            if (timeElement.isPresent()) {
                                String showTime = timeElement.get().getText().trim();
                                String language = languageElement.map(WebElement::getText).map(String::trim).orElse("");
                                
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
                                log.info("Found show: Movie='{}', Theatre='{}', Time='{}', Language='{}'", 
                                    movieName, theatreName, showTime, language);
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
            
            // Send notification if preferred theatre was found
            if (preferredTheatreFound) {
                sendTicketAvailabilityNotification(movieName, location, preferredTheatre, showTimings);
            } else {
                log.info("Preferred theatre '{}' not found for movie '{}'", preferredTheatre, movieName);
            }

        } catch (Exception e) {
            log.error("Major error during PVR scraping for movie '{}': {}", movieName, e.getMessage(), e);
        } finally {
            log.info("PVR scraping finished for movie '{}'. Found {} shows.", movieName, shows.size());
            // Consider closing the browser tab or navigating away if needed, but usually BaseScraper handles quit
        }
        return shows;
    }
    
    /**
     * Sends a notification when tickets become available
     */
    private void sendTicketAvailabilityNotification(String movieName, String location, String theatreName, List<String> showTimings) {
        try {
            log.info("Sending ticket availability notification for movie '{}' in theatre '{}' at location '{}'", 
                movieName, theatreName, location);
            
            String subject = String.format("Movie Alert: %s is now available at %s!", movieName, theatreName);
            
            // Format show timings for email body
            String showTimingsText;
            if (showTimings.isEmpty()) {
                showTimingsText = "No specific show timings available.";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Available show timings:\n");
                for (String time : showTimings) {
                    sb.append("- ").append(time).append("\n");
                }
                showTimingsText = sb.toString();
            }
            
            String body = String.format(
                "Dear Movie Fan,\n\n" +
                "Great news! The movie '%s' is now available for booking at %s in %s on %s.\n\n" +
                "%s\n\n" +
                "Don't miss out - book your tickets now!\n\n" +
                "Best regards,\nNotifyMe Team",
                movieName,
                theatreName,
                location,
                LocalDate.now(),
                showTimingsText
            );

            // Send email directly using EmailService
            emailService.sendEmail(recipientEmail, subject, body);
            
            log.info("Successfully sent ticket availability notification for movie '{}' at theatre '{}' to {}", 
                movieName, theatreName, recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send ticket availability notification for movie '{}': {}", movieName, e.getMessage());
        }
    }
}
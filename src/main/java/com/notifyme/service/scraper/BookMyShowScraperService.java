package com.notifyme.service.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyme.model.MovieShow;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BookMyShowScraperService implements WebScraperService {

    private static final String BASE_URL = "https://in.bookmyshow.com";
    private static final String SEARCH_URL = BASE_URL + "/search?q=%s";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<MovieShow> scrapeMovieShows(String movieName, String location) {
        List<MovieShow> shows = new ArrayList<>();
        try {
            String searchUrl = String.format(SEARCH_URL, movieName.replace(" ", "+"));
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .timeout(10000)
                    .get();

            Elements movieElements = doc.select(".movie-card");
            for (Element movieElement : movieElements) {
                try {
                    MovieShow show = parseMovieElement(movieElement, location);
                    if (show != null) {
                        shows.add(show);
                        logScrapedData(show);
                    }
                } catch (Exception e) {
                    log.error("Error parsing movie element: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error scraping BookMyShow: {}", e.getMessage());
        }
        return shows;
    }

    private MovieShow parseMovieElement(Element element, String location) {
        try {
            String movieName = element.select(".movie-name").text();
            String theaterName = element.select(".theater-name").text();
            String showTimeStr = element.select(".show-time").text();
            String priceRange = element.select(".price-range").text();
            String bookingUrl = BASE_URL + element.select("a").attr("href");

            LocalDateTime showTime = parseShowTime(showTimeStr);

            return MovieShow.builder()
                    .movieName(movieName)
                    .theaterName(theaterName)
                    .location(location)
                    .showTime(showTime)
                    .priceRange(priceRange)
                    .bookingUrl(bookingUrl)
                    .source("bookmyshow")
                    .isAvailable(true)
                    .scrapedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error parsing movie element: {}", e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseShowTime(String showTimeStr) {
        // Implementation will depend on the actual format of show times on BookMyShow
        // This is a placeholder implementation
        return LocalDateTime.now();
    }

    private void logScrapedData(MovieShow show) {
        try {
            String jsonData = objectMapper.writeValueAsString(show);
            log.info("Scraped data: {}", jsonData);
        } catch (Exception e) {
            log.error("Error logging scraped data: {}", e.getMessage());
        }
    }

    @Override
    public String getPlatformName() {
        return "bookmyshow";
    }
} 
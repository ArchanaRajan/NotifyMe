package com.notifyme.service.impl;

import com.notifyme.model.MovieShow;
import com.notifyme.scraper.BookMyShowScraper;
import com.notifyme.service.ScrapingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ScrapingServiceImpl implements ScrapingService {

    private final BookMyShowScraper bookMyShowScraper;

    @Autowired
    public ScrapingServiceImpl(BookMyShowScraper bookMyShowScraper) {
        this.bookMyShowScraper = bookMyShowScraper;
    }

    @Override
    public List<MovieShow> scrapeBookMyShowMovies(String movieName, String location) {
        log.info("Starting to scrape movies for movie: {} in location: {}", movieName, location);
        List<MovieShow> shows = bookMyShowScraper.scrapeMovieShows(movieName, location);
        log.info("Found {} shows for movie: {} in location: {}", shows.size(), movieName, location);
        return shows;
    }
}
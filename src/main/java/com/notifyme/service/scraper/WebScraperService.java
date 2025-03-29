package com.notifyme.service.scraper;

import com.notifyme.dto.MovieShow;
import java.util.List;

public interface WebScraperService {
    /**
     * Scrapes movie show data for a given movie and location
     * @param movieName Name of the movie to search for
     * @param location Location to search in
     * @return List of movie shows found
     */
    List<MovieShow> scrapeMovieShows(String movieName, String location);

    /**
     * Gets the name of the platform being scraped
     * @return Platform name
     */
    String getPlatformName();
} 
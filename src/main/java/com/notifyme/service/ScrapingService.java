package com.notifyme.service;

import com.notifyme.model.MovieShow;
import java.util.List;

public interface ScrapingService {
    List<MovieShow> scrapeBookMyShowMovies(String movieName, String location);
}
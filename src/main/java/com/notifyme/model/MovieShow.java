package com.notifyme.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MovieShow {
    private String movieName;
    private String theaterName;
    private String location;
    private LocalDateTime showTime;
    private String priceRange;
    private String bookingUrl;
    private String source;
    private boolean isAvailable;
    private LocalDateTime scrapedAt;
} 
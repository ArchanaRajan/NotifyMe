package com.notifyme.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SeleniumConfig {

    @Value("${selenium.headless:true}")
    private boolean headless;

    @Value("${selenium.user-agent}")
    private String userAgent;

    @Bean
    public WebDriver webDriver() {
        log.info("Initializing Chrome WebDriver with headless mode: {}", headless);
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=" + userAgent);
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        return new ChromeDriver(options);
    }
}
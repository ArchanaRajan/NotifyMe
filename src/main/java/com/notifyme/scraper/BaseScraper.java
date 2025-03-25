package com.notifyme.scraper;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public abstract class BaseScraper {

    protected final WebDriver webDriver;
    protected final WebDriverWait wait;

    @Value("${selenium.browser.timeout:10}")
    protected int timeout;

    protected BaseScraper(WebDriver webDriver) {
        this.webDriver = webDriver;
        this.wait = new WebDriverWait(webDriver, Duration.ofSeconds(timeout));
    }

    protected Optional<WebElement> findElement(By by) {
        try {
            return Optional.of(wait.until(ExpectedConditions.presenceOfElementLocated(by)));
        } catch (Exception e) {
            log.warn("Element not found: {}", by);
            return Optional.empty();
        }
    }

    protected List<WebElement> findElements(By by) {
        try {
            return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
        } catch (Exception e) {
            log.warn("Elements not found: {}", by);
            return List.of();
        }
    }

    protected boolean isElementVisible(By by) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by)).isDisplayed();
        } catch (Exception e) {
            log.warn("Element not visible: {}", by);
            return false;
        }
    }

    protected void waitForElementToBeClickable(By by) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(by));
        } catch (Exception e) {
            log.warn("Element not clickable: {}", by);
        }
    }

    protected void navigateTo(String url) {
        try {
            webDriver.get(url);
        } catch (Exception e) {
            log.error("Failed to navigate to URL: {}", url, e);
        }
    }

    protected void quit() {
        try {
            webDriver.quit();
        } catch (Exception e) {
            log.error("Failed to quit WebDriver", e);
        }
    }
}
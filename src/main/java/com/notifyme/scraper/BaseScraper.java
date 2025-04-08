package com.notifyme.scraper;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.NoSuchElementException;
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

    @Value("${selenium.browser.timeout:30}")
    protected int timeout;

    protected BaseScraper(WebDriver webDriver) {
        this.webDriver = webDriver;
        this.wait = new WebDriverWait(webDriver, Duration.ofSeconds(this.timeout));
    }

    protected Optional<WebElement> findElement(By by) {
        try {
            return Optional.of(wait.until(ExpectedConditions.presenceOfElementLocated(by)));
        } catch (Exception e) {
            log.warn("Element not found on page: {}", by);
            return Optional.empty();
        }
    }

    protected Optional<WebElement> findElementWithin(SearchContext parent, By by) {
        try {
            return Optional.of(parent.findElement(by));
        } catch (NoSuchElementException e) {
            log.trace("Element not found within parent: {}", by);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error finding element within parent: {} - Error: {}", by, e.getMessage());
            return Optional.empty();
        }
    }

    protected Optional<WebElement> findElementWithin(SearchContext parent, By by, int customTimeoutSeconds) {
        WebDriverWait customWait = new WebDriverWait(webDriver, Duration.ofSeconds(customTimeoutSeconds));
        try {
            WebElement element = customWait.until(ExpectedConditions.presenceOfElementLocated(by));
            if (parent instanceof WebElement && ((WebElement)parent).findElements(By.xpath(".//*[."+ by.toString() + "]")).contains(element)){
                return Optional.of(element);
            } else if (parent == webDriver) {
                return Optional.of(element);
            } else {
                log.trace("Element {} found globally but not within the specific parent context with custom wait.", by);
                return Optional.empty();
            }
        } catch (org.openqa.selenium.TimeoutException e) {
            log.trace("Element not found within parent {} with custom timeout {}s: {}", parent, customTimeoutSeconds, by);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error finding element within parent with custom wait: {} - Error: {}", by, e.getMessage());
            return Optional.empty();
        }
    }

    protected List<WebElement> findElements(By by) {
        try {
            return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
        } catch (Exception e) {
            log.warn("Elements not found on page: {}", by);
            return List.of();
        }
    }

    protected List<WebElement> findElementsWithin(SearchContext parent, By by) {
        try {
            return parent.findElements(by);
        } catch (Exception e) {
            log.warn("Error finding elements within parent: {} - Error: {}", by, e.getMessage());
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
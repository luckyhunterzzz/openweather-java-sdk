package org.openweather.sdk.core;

import org.openweather.sdk.api.OpenWeatherApi;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.exception.WeatherApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the PollingService, utilizing a ScheduledExecutorService for
 * periodic updates of cached weather data.
 */
public class PollingServiceImpl implements PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingServiceImpl.class);

    private final OpenWeatherApi apiClient;
    private final WeatherCacheManager cacheManager;
    private final ScheduledExecutorService scheduler;
    private final long pollingIntervalMillis;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Initializes the Polling Service.
     *
     * @param apiClient The client for interacting with the external OpenWeather API.
     * @param cacheManager The cache manager.
     * @param pollingInterval The interval for polling updates.
     */
    public PollingServiceImpl(OpenWeatherApi apiClient, WeatherCacheManager cacheManager, Duration pollingInterval) {
        if (pollingInterval == null || pollingInterval.isNegative() || pollingInterval.isZero()) {
            log.error("Polling interval must be a positive duration. Provided: {}", pollingInterval);
            throw new IllegalArgumentException("Polling interval must be a positive duration.");
        }
        this.apiClient = apiClient;
        this.cacheManager = cacheManager;
        this.pollingIntervalMillis = pollingInterval.toMillis();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("Weather-Polling-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Defines the scheduled task that performs polling:
     * 1. Explicitly evicts expired entries from the cache.
     * 2. Updates the weather data for all remaining cached cities.
     */
    private Runnable getPollingTask() {
        return () -> {
            try {
                cacheManager.evictExpired();

                Set<String> citiesToUpdate = cacheManager.getCachedCities();

                if (citiesToUpdate.isEmpty()) {
                    log.info("Cache is empty. No cities to update.");
                    return;
                }

                log.info("Starting update for {} cities: {}", citiesToUpdate.size(), citiesToUpdate);

                for (String city : citiesToUpdate) {
                    try {
                        var response = apiClient.getWeather(city);
                        cacheManager.put(city, response);
                        log.info("Successfully updated weather for {}.", city);
                    } catch (WeatherApiException e) {
                        log.error("Failed to update weather for {}. Error: {}", city, e.getMessage());
                    } catch (Exception e) {
                        log.error("Unexpected error during update for {}.", city, e);
                    }
                }
            } catch (Exception e) {
                log.error("Polling Task encountered a critical error.", e);
            }
        };
    }

    /**
     * {@inheritDoc}
     * Starts the scheduled background task for periodically updating cached weather data.
     * <p>
     * This method is designed to be idempotent; if the polling service is already running,
     * this call will be ignored, preventing the creation of duplicate scheduled tasks.
     * The polling interval is determined by the value provided during service initialization.
     * </p>
     */
    @Override
    public void startPolling() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleWithFixedDelay(
                    getPollingTask(),
                    0,
                    pollingIntervalMillis,
                    TimeUnit.MILLISECONDS
            );
            log.info("Polling Service started. Update interval: {} ms.", pollingIntervalMillis);
        } else {
            log.warn("Polling Service is already running, ignoring duplicate start call.");
        }
    }

    /**
     * {@inheritDoc}
     * Initiates a graceful shutdown of the underlying scheduled executor service.
     * <p>
     * This method attempts to stop all executing polling tasks and waits up to 30 seconds
     * for their completion. If the termination timeout is reached, the scheduler is
     * forcibly shut down using {@code shutdownNow()}.
     * </p>
     * The service is marked as stopped, ensuring it can be safely restarted later.
     */
    @Override
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            log.info("Polling Service shutting down...");
            try {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    log.warn("Polling Service forcibly terminated.");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("Polling Service shutdown was interrupted.", e);
            }
            log.info("Polling Service stopped.");
        } else {
            log.info("Polling Service was already stopped or never started.");
        }
    }
}
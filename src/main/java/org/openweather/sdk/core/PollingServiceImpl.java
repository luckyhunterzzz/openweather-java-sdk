package org.openweather.sdk.core;

import org.openweather.sdk.api.OpenWeatherApi;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.exception.WeatherApiException;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Реализация PollingService, использующая ScheduledExecutorService для
 * периодического обновления кэшированных данных.
 */
public class PollingServiceImpl implements PollingService {

    private final OpenWeatherApi apiClient;
    private final WeatherCacheManager cacheManager;
    private final ScheduledExecutorService scheduler;
    private final long pollingIntervalSeconds;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Инициализирует Polling Service.
     *
     * @param apiClient Клиент для взаимодействия с внешним API.
     * @param cacheManager Менеджер кэша.
     * @param pollingInterval Интервал опроса.
     */
    public PollingServiceImpl(OpenWeatherApi apiClient, WeatherCacheManager cacheManager, Duration pollingInterval) {
        if (pollingInterval == null || pollingInterval.isNegative() || pollingInterval.isZero()) {
            throw new IllegalArgumentException("Polling interval must be a positive duration.");
        }
        this.apiClient = apiClient;
        this.cacheManager = cacheManager;
        this.pollingIntervalSeconds = pollingInterval.getSeconds();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("Weather-Polling-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Задача, которая выполняет опрос:
     * 1. Принудительно очищает кэш от устаревших данных.
     * 2. Обновляет все оставшиеся города.
     */
    private Runnable getPollingTask() {
        return () -> {
            try {
                cacheManager.evictExpired();

                Set<String> citiesToUpdate = cacheManager.getCachedCities();

                if (citiesToUpdate.isEmpty()) {
                    System.out.println("Polling: Cache is empty. No cities to update.");
                    return;
                }

                System.out.printf("Polling: Starting update for %d cities: %s\n", citiesToUpdate.size(), citiesToUpdate);

                for (String city : citiesToUpdate) {
                    try {
                        var response = apiClient.getWeather(city);
                        cacheManager.put(city, response);
                        System.out.printf("Polling: Successfully updated weather for %s.\n", city);
                    } catch (WeatherApiException e) {
                        System.err.printf("Polling: Failed to update weather for %s. Error: %s\n", city, e.getMessage());
                    } catch (Exception e) {
                        System.err.printf("Polling: Unexpected error during update for %s. Error: %s\n", city, e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Polling Task encountered a critical error: " + e.getMessage());
            }
        };
    }

    /**
     * Потом
     * Защищен от двойного запуска.
     */
    @Override
    public void startPolling() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleWithFixedDelay(
                    getPollingTask(),
                    0,
                    pollingIntervalSeconds,
                    TimeUnit.SECONDS
            );
            System.out.printf("INFO: Polling Service started. Update interval: %d seconds.\n", pollingIntervalSeconds);
        } else {
            System.out.println("WARNING: Polling Service is already running, ignoring duplicate start call.");
        }
    }

    /**
     * Потом
     */
    @Override
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            System.out.println("INFO: Polling Service shutting down...");
            try {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    System.out.println("WARNING: Polling Service forcibly terminated.");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("INFO: Polling Service stopped.");
        } else {
            System.out.println("INFO: Polling Service was already stopped or never started.");
        }
    }
}
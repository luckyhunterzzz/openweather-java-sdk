package org.openweather.sdk.core;

import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;

/**
 * Public contract (interface) for accessing the core functionality of the SDK.
 * Provides the main method for retrieving weather data and a method for graceful shutdown.
 */
public interface WeatherSDK {

    /**
     * Retrieves the current weather information for the specified city.
     * Implements caching logic: first checks the cache, then, if necessary,
     * calls the external API.
     *
     * @param city The name of the city (e.g., "Moscow", "Tokyo").
     * @return A WeatherResponse object with the weather data.
     * @throws WeatherApiException If an error occurs during API interaction or processing.
     */
    WeatherResponse getCurrentWeather(String city) throws WeatherApiException;

    /**
     * Initiates a graceful shutdown of the SDK, including stopping all background
     * threads and services (e.g., PollingService).
     */
    void shutdown();
}

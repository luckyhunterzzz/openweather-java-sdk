package org.openweather.sdk.api;

import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;

/**
 * Public contract (interface) for accessing core functionality of the OpenWeather API.
 * Defines the method for fetching current weather data.
 */
public interface OpenWeatherApi {
    /**
     * Retrieves current weather data for the specified city.
     *
     * @param cityName The name of the city (e.g., "London", "Paris").
     * @return The parsed WeatherResponse object.
     * @throws WeatherApiException In case of an API error (e.g., invalid key, city not found, network issues).
     */
    WeatherResponse getWeather(String cityName) throws WeatherApiException;
}
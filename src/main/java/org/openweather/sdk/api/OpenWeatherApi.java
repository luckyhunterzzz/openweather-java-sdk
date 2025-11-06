package org.openweather.sdk.api;

import org.openweather.sdk.exception.WeatherApiException;

public interface OpenWeatherApi {
    String getRawWeatherJson(String cityName) throws WeatherApiException;
}
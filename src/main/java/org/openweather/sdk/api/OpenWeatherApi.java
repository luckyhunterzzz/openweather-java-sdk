package org.openweather.sdk.api;

import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;

public interface OpenWeatherApi {
    /**
     * Получает текущие погодные данные для указанного города.
     * @param cityName Название города.
     * @return Обработанный объект WeatherResponse.
     * @throws WeatherApiException В случае ошибки API (например, неверный ключ, город не найден).
     */
    WeatherResponse getWeather(String cityName) throws WeatherApiException;
}
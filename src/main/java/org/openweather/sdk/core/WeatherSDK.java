package org.openweather.sdk.core;

import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;

/**
 * Публичный контракт (интерфейс) для доступа к функциональности SDK.
 * Предоставляет основной метод получения погоды и метод для корректного завершения работы.
 */
public interface WeatherSDK {

    /**
     * Получает актуальную информацию о погоде для указанного города.
     * Реализует логику кэширования: сначала проверяет кэш, затем, при необходимости,
     * обращается к внешнему API.
     *
     * @param city Название города (например, "Moscow", "Tokyo").
     * @return Объект WeatherResponse с данными о погоде.
     * @throws WeatherApiException Если произошла ошибка при работе с API.
     */
    WeatherResponse getCurrentWeather(String city) throws WeatherApiException;

    /**
     * Вызывает корректное завершение работы SDK, включая остановку всех фоновых
     * потоков и служб (например, PollingService).
     */
    void shutdown();
}

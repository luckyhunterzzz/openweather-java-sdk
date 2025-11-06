package org.openweather.sdk.core;

import org.openweather.sdk.api.OpenWeatherApi;
import org.openweather.sdk.api.OpenWeatherApiClient;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.SdkMode;
import org.openweather.sdk.model.WeatherResponse;

import java.util.Optional;

/**
 * Основная реализация интерфейса WeatherSDK.
 * Отвечает за интеграцию API-клиента и кэш-менеджера, а также за управление
 * режимами работы (ON_DEMAND и POLLING).
 */
public class WeatherSDKImpl implements WeatherSDK {

    private final OpenWeatherApi apiClient;
    private final WeatherCacheManager cacheManager;
    private final SdkMode mode;
    //TODO: PollingService будет добавлен позже, в отдельной задаче.

    /**
     * Создает новый экземпляр WeatherSDKImpl.
     *
     * @param apiKey API-ключ OpenWeatherMap (используется для инициализации OpenWeatherApi).
     * @param mode Режим работы SDK (ON_DEMAND или POLLING).
     * @param cacheManager Менеджер кэша.
     */
    public WeatherSDKImpl(String apiKey, SdkMode mode, WeatherCacheManager cacheManager) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key cannot be null or empty.");
        }
        if (mode == null) {
            throw new IllegalArgumentException("SdkMode cannot be null.");
        }
        this.apiClient = new OpenWeatherApiClient(apiKey);
        this.cacheManager = cacheManager;
        this.mode = mode;

        if (mode == SdkMode.POLLING) {
            //TODO: Здесь в будущей задаче будет инициализация PollingService
            System.out.println("INFO: SDK initialized in POLLING mode. Polling service not yet active.");
        }
    }

    /**
     * Добавлю как все закончу
     */
    @Override
    public WeatherResponse getCurrentWeather(String city) throws WeatherApiException {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be null or empty.");
        }

        Optional<WeatherResponse> cachedResponse = cacheManager.getActual(city);

        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        WeatherResponse apiResponse;
        try {
            apiResponse = apiClient.getWeather(city);
        } catch (WeatherApiException e) {
            throw e;
        }

        cacheManager.put(city, apiResponse);

        return apiResponse;
    }

    /**
     * Добавлю как все закончу
     *
     * В режиме ON_DEMAND ничего не делает.
     * В режиме POLLING остановит фоновый поток опроса.
     */
    @Override
    public void shutdown() {
        if (mode == SdkMode.POLLING) {
            //TODO: На след таску
            System.out.println("INFO: Polling service shutting down...");
        }
        System.out.println("INFO: WeatherSDK instance shut down.");
    }

    public SdkMode getMode() {
        return mode;
    }

    public WeatherCacheManager getCacheManager() {
        return cacheManager;
    }
}
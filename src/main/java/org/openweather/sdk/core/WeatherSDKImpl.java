package org.openweather.sdk.core;

import org.openweather.sdk.api.OpenWeatherApi;
import org.openweather.sdk.api.OpenWeatherApiClient;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.SdkMode;
import org.openweather.sdk.model.WeatherResponse;

import java.time.Duration;
import java.util.Optional;

/**
 * Основная реализация интерфейса WeatherSDK.
 * Отвечает за интеграцию API-клиента и кэш-менеджера, а также за управление
 * режимами работы (ON_DEMAND и POLLING).
 */
public class WeatherSDKImpl implements WeatherSDK {

    private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofMinutes(8);

    private final OpenWeatherApi apiClient;
    private final WeatherCacheManager cacheManager;
    private final SdkMode mode;
    private final PollingService pollingService;

    /**
     * Создает новый экземпляр WeatherSDKImpl с настраиваемым интервалом опроса.
     *
     * @param apiKey API-ключ OpenWeatherMap.
     * @param mode Режим работы SDK (ON_DEMAND или POLLING).
     * @param cacheManager Менеджер кэша.
     * @param pollingInterval Пользовательский интервал для режима POLLING (может быть null).
     */
    public WeatherSDKImpl(
            final String apiKey,
            final SdkMode mode,
            final WeatherCacheManager cacheManager,
            final Duration pollingInterval) {

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
            Duration effectiveInterval = (pollingInterval != null) ? pollingInterval : DEFAULT_POLLING_INTERVAL;

            this.pollingService = new PollingServiceImpl(this.apiClient, this.cacheManager, effectiveInterval);
            this.pollingService.startPolling();
        } else {
            this.pollingService = null;
        }
    }

    /**
     * Создает новый экземпляр WeatherSDKImpl с интервалом опроса по умолчанию.
     */
    public WeatherSDKImpl(final String apiKey, final SdkMode mode, final WeatherCacheManager cacheManager) {
        this(apiKey, mode, cacheManager, null);
    }

    /**
     * Добавлю как все закончу
     */
    @Override
    public WeatherResponse getCurrentWeather(final String city) throws WeatherApiException {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be null or empty.");
        }

        final Optional<WeatherResponse> cachedResponse = cacheManager.getActual(city);

        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        final WeatherResponse apiResponse;
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
        if (pollingService != null) {
            pollingService.shutdown();
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
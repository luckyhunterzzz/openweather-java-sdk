package org.openweather.sdk.core;

import org.openweather.sdk.api.OpenWeatherApi;
import org.openweather.sdk.api.OpenWeatherApiClient;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.SdkMode;
import org.openweather.sdk.model.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * The main implementation of the WeatherSDK interface.
 * Responsible for integrating the API client and cache manager, as well as managing
 * operational modes (ON_DEMAND and POLLING).
 */
public class WeatherSDKImpl implements WeatherSDK {

    private static final Logger log = LoggerFactory.getLogger(WeatherSDKImpl.class);

    private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofMinutes(8);

    private final OpenWeatherApi apiClient;
    private final WeatherCacheManager cacheManager;
    private final SdkMode mode;
    private final PollingService pollingService;

    /**
     * Creates a new instance of WeatherSDKImpl with a customizable polling interval.
     *
     * @param apiKey OpenWeatherMap API key.
     * @param mode SDK operational mode (ON_DEMAND or POLLING).
     * @param cacheManager The cache manager instance.
     * @param pollingInterval Custom interval for POLLING mode (can be null, defaulting to 8 minutes).
     */
    public WeatherSDKImpl(
            final String apiKey,
            final SdkMode mode,
            final WeatherCacheManager cacheManager,
            final Duration pollingInterval) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("API Key cannot be null or empty.");
            throw new IllegalArgumentException("API Key cannot be null or empty.");
        }
        if (mode == null) {
            log.error("SdkMode cannot be null.");
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
     * Creates a new instance of WeatherSDKImpl with the default polling interval.
     */
    public WeatherSDKImpl(final String apiKey, final SdkMode mode, final WeatherCacheManager cacheManager) {
        this(apiKey, mode, cacheManager, null);
    }

    /**
     * {@inheritDoc}
     *
     * Retrieves the current weather for the specified city.
     * First checks the cache. If the data is actual, it returns it.
     * If not, it requests the data via the API and caches the result.
     *
     * @param city The name of the city.
     * @return WeatherResponse object with current weather data.
     * @throws WeatherApiException If an error occurs during API interaction.
     * @throws IllegalArgumentException If the city name is null or empty.
     */
    @Override
    public WeatherResponse getCurrentWeather(final String city) throws WeatherApiException {
        if (city == null || city.trim().isEmpty()) {
            log.warn("Attempted to get weather with null or empty city name.");
            throw new IllegalArgumentException("City name cannot be null or empty.");
        }

        log.debug("Attempting to get weather for {}.", city);
        final Optional<WeatherResponse> cachedResponse = cacheManager.getActual(city);

        if (cachedResponse.isPresent()) {
            log.info("Cache hit for {}. Returning cached data.", city);
            return cachedResponse.get();
        }

        log.info("Cache miss for {}. Calling OpenWeather API.", city);
        final WeatherResponse apiResponse;
        try {
            apiResponse = apiClient.getWeather(city);
        } catch (WeatherApiException e) {
            log.error("API call failed for {}. Error: {}", city, e.getMessage());
            throw e;
        }

        cacheManager.put(city, apiResponse);
        log.info("Successfully fetched and cached new weather data for {}.", city);

        return apiResponse;
    }

    /**
     * {@inheritDoc}
     *
     * Performs a graceful shutdown of the SDK instance.
     *
     * In ON_DEMAND mode, it only logs the action.
     * In POLLING mode, it stops the background polling thread.
     */
    @Override
    public void shutdown() {
        if (pollingService != null) {
            pollingService.shutdown();
        }
        log.info("WeatherSDK instance shut down in {} mode.", mode);
    }

    /**
     * Returns the current operational mode of the SDK instance.
     *
     * @return The SdkMode (ON_DEMAND or POLLING).
     */
    public SdkMode getMode() {
        return mode;
    }

    /**
     * Returns the underlying cache manager used by this SDK instance.
     *
     * @return The WeatherCacheManager instance.
     */
    public WeatherCacheManager getCacheManager() {
        return cacheManager;
    }
}
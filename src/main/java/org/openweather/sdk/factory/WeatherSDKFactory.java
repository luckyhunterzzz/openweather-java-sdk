package org.openweather.sdk.factory;

import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.core.WeatherSDK;
import org.openweather.sdk.core.WeatherSDKImpl;
import org.openweather.sdk.model.SdkMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe factory for managing WeatherSDK instances.
 * Implements the Singleton-Per-Key pattern: only one WeatherSDK instance
 * is created for each unique API Key.
 *
 * Uses ConcurrentMap.computeIfAbsent to ensure atomic creation
 * and prevent race conditions.
 */
public final class WeatherSDKFactory {

    private static final Logger log = LoggerFactory.getLogger(WeatherSDKFactory.class);

    private static final ConcurrentMap<String, WeatherSDK> instances = new ConcurrentHashMap<>();

    private WeatherSDKFactory() {
    }

    /**
     * Returns an existing WeatherSDK instance for the given API Key or creates a new one.
     * Uses default cache manager and polling interval.
     *
     * @param apiKey The OpenWeatherMap API key.
     * @param mode The operational mode of the SDK (ON_DEMAND or POLLING).
     * @return The single WeatherSDK instance associated with this key.
     * @throws IllegalArgumentException if the API key or mode is invalid.
     */
    public static WeatherSDK getOrCreateSDK(final String apiKey, final SdkMode mode) {
        return getOrCreateSDK(apiKey, mode, null, null);
    }

    /**
     * Returns an existing WeatherSDK instance for the given API Key or creates a new one.
     *
     * @param apiKey The OpenWeatherMap API key.
     * @param mode The operational mode of the SDK (ON_DEMAND or POLLING).
     * @param cacheManager An optional custom cache manager (if null, a default one will be created).
     * @param pollingInterval An optional custom interval for POLLING mode (if null, a default one will be used).
     * @return The single WeatherSDK instance associated with this key.
     * @throws IllegalArgumentException if the API key or mode is invalid.
     */
    public static WeatherSDK getOrCreateSDK(
            final String apiKey,
            final SdkMode mode,
            final WeatherCacheManager cacheManager,
            final Duration pollingInterval) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Attempted to create SDK with null or empty API Key.");
            throw new IllegalArgumentException("API Key cannot be null or empty.");
        }
        if (mode == null) {
            log.error("Attempted to create SDK with null SdkMode for key: {}...",
                    apiKey.substring(0, Math.min(4, apiKey.length())));
            throw new IllegalArgumentException("SdkMode cannot be null.");
        }

        return instances.computeIfAbsent(apiKey, key -> {
            log.info("Creating new WeatherSDK instance for API Key: {}...",
                    apiKey.substring(0, Math.min(4, apiKey.length())));

            final WeatherCacheManager effectiveCacheManager =
                    (cacheManager != null) ? cacheManager : new WeatherCacheManager();

            return new WeatherSDKImpl(key, mode, effectiveCacheManager, pollingInterval);
        });
    }

    /**
     * Gracefully shuts down the SDK instance associated with the given API Key
     * and removes it from the factory.
     *
     * @param apiKey The API key of the instance to release.
     * @return true if the instance was found and shut down; false if not found.
     */
    public static boolean releaseSDK(final String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        final WeatherSDK sdkToRelease = instances.remove(apiKey);

        if (sdkToRelease != null) {
            log.info("Shutting down and releasing WeatherSDK instance for API Key: {}...",
                    apiKey.substring(0, Math.min(4, apiKey.length())));
            sdkToRelease.shutdown();
            return true;
        } else {
            log.warn("WeatherSDK instance for API Key: {}... not found or already released.",
                    apiKey.substring(0, Math.min(4, apiKey.length())));
            return false;
        }
    }

    /**
     * Returns the count of active SDK instances.
     * Used for debugging and testing purposes.
     *
     * @return The number of currently active SDK instances.
     */
    public static int getActiveInstanceCount() {
        return instances.size();
    }
}
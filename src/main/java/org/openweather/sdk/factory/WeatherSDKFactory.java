package org.openweather.sdk.factory;

import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.core.WeatherSDK;
import org.openweather.sdk.core.WeatherSDKImpl;
import org.openweather.sdk.model.SdkMode;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Потокобезопасная фабрика для управления экземплярами WeatherSDK.
 * Реализует паттерн Singleton-Per-Key: для каждого уникального API Key
 * создается только один экземпляр WeatherSDK.
 *
 * Использует ConcurrentMap.computeIfAbsent для обеспечения атомарности
 * создания и предотвращения гонок потоков.
 */
public final class WeatherSDKFactory {

    private static final ConcurrentMap<String, WeatherSDK> instances = new ConcurrentHashMap<>();

    private WeatherSDKFactory() {
    }

    /**
     * Возвращает существующий экземпляр WeatherSDK для данного API Key или создает новый.
     * Использует кэш-менеджер и интервал опроса по умолчанию.
     *
     * @param apiKey API-ключ OpenWeatherMap.
     * @param mode Режим работы SDK (ON_DEMAND или POLLING).
     * @return Единственный экземпляр WeatherSDK, связанный с данным ключом.
     */
    public static WeatherSDK getOrCreateSDK(final String apiKey, final SdkMode mode) {
        return getOrCreateSDK(apiKey, mode, null, null);
    }

    /**
     * Возвращает существующий экземпляр WeatherSDK для данного API Key или создает новый.
     *
     * @param apiKey API-ключ OpenWeatherMap.
     * @param mode Режим работы SDK (ON_DEMAND или POLLING).
     * @param cacheManager Менеджер кэша (может быть null, будет создан дефолтный).
     * @param pollingInterval Пользовательский интервал для режима POLLING (может быть null).
     * @return Единственный экземпляр WeatherSDK, связанный с данным ключом.
     */
    public static WeatherSDK getOrCreateSDK(
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

        return instances.computeIfAbsent(apiKey, key -> {
            System.out.println("INFO: Creating new WeatherSDK instance for API Key: " + apiKey.substring(0, 4) + "...");

            final WeatherCacheManager effectiveCacheManager =
                    (cacheManager != null) ? cacheManager : new WeatherCacheManager();

            return new WeatherSDKImpl(key, mode, effectiveCacheManager, pollingInterval);
        });
    }

    /**
     * Корректно останавливает работу экземпляра SDK, связанного с данным API Key,
     * и удаляет его из фабрики.
     *
     * @param apiKey API-ключ экземпляра, который нужно освободить.
     * @return true, если экземпляр был найден и остановлен; false, если не найден.
     */
    public static boolean releaseSDK(final String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        final WeatherSDK sdkToRelease = instances.remove(apiKey);

        if (sdkToRelease != null) {
            System.out.println("INFO: Shutting down and releasing WeatherSDK instance for API Key: " + apiKey.substring(0, 4) + "...");
            sdkToRelease.shutdown();
            return true;
        } else {
            System.out.println("WARNING: WeatherSDK instance for API Key not found or already released.");
            return false;
        }
    }

    /**
     * Возвращает количество активных экземпляров SDK.
     * Используется для отладки и тестов.
     */
    public static int getActiveInstanceCount() {
        return instances.size();
    }
}
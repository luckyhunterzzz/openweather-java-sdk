package org.openweather.sdk.cache;

import org.openweather.sdk.model.WeatherResponse;

/**
 * Обертка для кэшированных данных о погоде.
 * Хранит сам объект ответа и момент времени, когда он был создан/обновлен,
 * для проверки актуальности (TTL - Time-To-Live).
 */
public record CacheEntry(
        /** Объект ответа погоды. */
        WeatherResponse weatherResponse,

        /** Момент времени (UNIX timestamp в миллисекундах), когда данные были помещены в кэш. */
        long timestamp
) {
    /**
     * Проверяет, устарели ли данные.
     * @param maxAgeMillis максимальное допустимое время жизни данных в миллисекундах.
     * @return true, если данные устарели (время с момента timestamp превышает maxAgeMillis).
     */
    public boolean isExpired(long maxAgeMillis) {
        return System.currentTimeMillis() > (timestamp + maxAgeMillis);
    }
}

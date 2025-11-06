package org.openweather.sdk.cache;

import org.openweather.sdk.model.WeatherResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Менеджер кэша для хранения погодных данных.
 * Реализует логику LRU (Least Recently Used) для ограничения размера
 * и проверку актуальности данных (TTL - Time-To-Live).
 * <p>
 * Использует ReadWriteLock для обеспечения потокобезопасности при работе с кэшем.
 */
public class WeatherCacheManager {

    private static final int DEFAULT_MAX_CACHE_SIZE = 10;
    private static final long DEFAULT_MAX_AGE_MILLIS = 10 * 60 * 1000;

    private final int maxCacheSize;
    private final long maxAgeMillis;

    private final Map<String, CacheEntry> cache;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Конструктор с параметрами по умолчанию (10 городов, 10 минут TTL).
     */
    public WeatherCacheManager() {
        this(DEFAULT_MAX_CACHE_SIZE, DEFAULT_MAX_AGE_MILLIS);
    }

    /**
     * Конструктор с возможностью задания размера кэша и времени жизни.
     * @param maxCacheSize максимальное количество элементов в кэше.
     * @param maxAgeMillis максимальное допустимое время жизни данных в миллисекундах.
     */
    public WeatherCacheManager(int maxCacheSize, long maxAgeMillis) {
        if (maxCacheSize <= 0 || maxAgeMillis <= 0) {
            throw new IllegalArgumentException("Cache size and max age must be positive.");
        }
        this.maxCacheSize = maxCacheSize;
        this.maxAgeMillis = maxAgeMillis;

        this.cache = new LinkedHashMap<>(maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxCacheSize;
            }
        };
    }

    /**
     * Пытается получить актуальные данные из кэша.
     * Если данные есть, но устарели (старше maxAgeMillis), они удаляются и возвращается Optional.empty().
     *
     * @param city Название города.
     * @return Optional с актуальными данными или Optional.empty().
     */
    public Optional<WeatherResponse> getActual(String city) {
        String key = city.toLowerCase();
        CacheEntry entry;

        lock.readLock().lock();
        try {
            entry = cache.get(key);

            if (entry == null) {
                return Optional.empty();
            }

            if (!entry.isExpired(maxAgeMillis)) {
                return Optional.of(entry.weatherResponse());
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            CacheEntry latestEntry = cache.get(key);

            if (latestEntry != null && latestEntry.isExpired(maxAgeMillis)) {
                cache.remove(key);
            }
        } finally {
            lock.writeLock().unlock();
        }

        return Optional.empty();
    }

    /**
     * Добавляет или обновляет данные о погоде в кэше.
     *
     * @param city Название города.
     * @param response Объект WeatherResponse, полученный от API.
     */
    public void put(String city, WeatherResponse response) {
        lock.writeLock().lock();
        try {
            String key = city.toLowerCase();
            CacheEntry newEntry = new CacheEntry(response, System.currentTimeMillis());
            cache.put(key, newEntry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Принудительно удаляет все устаревшие записи из кэша.
     * Полезно для фоновых служб (PollingService) перед обновлением.
     */
    public void evictExpired() {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(maxAgeMillis));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Получает список всех кэшированных городов.
     * Используется службой PollingService для фонового обновления.
     *
     * @return Неизменяемый Set с названиями городов (ключи кэша).
     */
    public Set<String> getCachedCities() {
        lock.readLock().lock();
        try {
            return Set.copyOf(cache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Очищает весь кэш.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Возвращает текущий размер кэша. Полезно для отладки и тестирования.
     * @return Текущее количество элементов в кэше.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}

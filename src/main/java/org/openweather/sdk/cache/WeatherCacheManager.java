package org.openweather.sdk.cache;

import org.openweather.sdk.model.WeatherResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Cache Manager for storing weather data.
 * Implements LRU (Least Recently Used) eviction logic to limit size
 * and checks data freshness (TTL - Time-To-Live).
 * <p>
 * Uses ReadWriteLock for thread-safe concurrent access to the cache.
 */
public class WeatherCacheManager {

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheManager.class);

    private static final int DEFAULT_MAX_CACHE_SIZE = 10;
    private static final long DEFAULT_MAX_AGE_MILLIS = 10 * 60 * 1000;

    private final int maxCacheSize;
    private final long maxAgeMillis;

    private final Map<String, CacheEntry> cache;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructor with default parameters (10 cities, 10 minutes TTL).
     */
    public WeatherCacheManager() {
        this(DEFAULT_MAX_CACHE_SIZE, DEFAULT_MAX_AGE_MILLIS);
    }

    /**
     * Constructor allowing custom cache size and time-to-live configuration.
     * @param maxCacheSize The maximum number of elements the cache can hold.
     * @param maxAgeMillis The maximum allowed time-to-live for data, in milliseconds.
     * @throws IllegalArgumentException if cache size or max age is not positive.
     */
    public WeatherCacheManager(int maxCacheSize, long maxAgeMillis) {
        if (maxCacheSize <= 0 || maxAgeMillis <= 0) {
            log.error("Cache size ({}) and max age ({}) must be positive.", maxCacheSize, maxAgeMillis);
            throw new IllegalArgumentException("Cache size and max age must be positive.");
        }
        this.maxCacheSize = maxCacheSize;
        this.maxAgeMillis = maxAgeMillis;
        log.info("Initialized WeatherCacheManager: size {} elements, TTL {} ms.", maxCacheSize, maxAgeMillis);

        this.cache = new LinkedHashMap<>(maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                boolean shouldRemove = size() > maxCacheSize;
                if (shouldRemove) {
                    log.debug("LRU eviction triggered. Removing eldest entry: {}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
    }

    /**
     * Attempts to retrieve actual (non-expired) data from the cache.
     * If data exists but is expired (older than maxAgeMillis), it is removed from the cache,
     * and Optional.empty() is returned.
     *
     * @param city The name of the city.
     * @return An Optional containing actual data, or Optional.empty().
     */
    public Optional<WeatherResponse> getActual(String city) {
        String key = city.toLowerCase();
        CacheEntry entry;

        lock.readLock().lock();
        try {
            entry = cache.get(key);

            if (entry == null) {
                log.debug("Cache miss for {}.", city);
                return Optional.empty();
            }

            if (!entry.isExpired(maxAgeMillis)) {
                log.debug("Cache hit for {}. Data is actual.", city);
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
                log.info("Entry for {} expired (TTL: {} ms) and was removed.", city, maxAgeMillis);
            }
        } finally {
            lock.writeLock().unlock();
        }

        return Optional.empty();
    }

    /**
     * Adds or updates weather data in the cache.
     *
     * @param city The name of the city.
     * @param response The WeatherResponse object received from the API.
     */
    public void put(String city, WeatherResponse response) {
        lock.writeLock().lock();
        try {
            String key = city.toLowerCase();
            CacheEntry newEntry = new CacheEntry(response, System.currentTimeMillis());
            cache.put(key, newEntry);
            log.debug("Data saved/updated for {}.", city);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Forcibly removes all expired entries from the cache.
     * Useful for background services (PollingService) before performing updates.
     */
    public void evictExpired() {
        lock.writeLock().lock();
        try {
            int initialSize = cache.size();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(maxAgeMillis));
            int removedCount = initialSize - cache.size();
            log.info("Finished forced eviction of expired entries. Removed {} elements.", removedCount);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a set of all currently cached city names (keys).
     * Used by the PollingService for background updates.
     *
     * @return An immutable Set of city names (cache keys).
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
     * Clears the entire cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            log.info("Cache fully cleared.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the current size of the cache. Useful for debugging and testing.
     * @return The current number of elements in the cache.
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

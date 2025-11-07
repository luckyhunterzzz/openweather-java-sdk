package org.openweather.sdk.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openweather.sdk.model.*;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link WeatherCacheManager}.
 * <p>
 * Verifies correct cache storage, expiration, eviction (LRU), and immutability behavior.
 */
class WeatherCacheManagerTest {

    private WeatherCacheManager cacheManager;

    private static final WeatherResponse SAMPLE_RESPONSE = new WeatherResponse(
            new Weather("Clouds", "scattered clouds"),
            new Temperature(280.0, 278.5),
            10000,
            new Wind(3.5),
            123456789L,
            new Sys(1600000000L, 1600030000L),
            3600,
            "London"
    );

    @BeforeEach
    void setUp() {
        cacheManager = new WeatherCacheManager(3, 200);
    }

    @Test
    @DisplayName("Should throw exception when constructor arguments are invalid")
    void shouldThrowExceptionWhen_ConstructorArgumentsAreInvalid() {
        assertThatThrownBy(() -> new WeatherCacheManager(0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> new WeatherCacheManager(5, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should store and retrieve item when entry is not expired")
    void shouldStoreAndRetrieveItemWhen_EntryIsNotExpired() {
        cacheManager.put("London", SAMPLE_RESPONSE);

        Optional<WeatherResponse> actual = cacheManager.getActual("London");

        assertThat(actual).isPresent();
        assertThat(actual.get().name()).isEqualTo("London");
    }

    @Test
    @DisplayName("Should return empty when no such city is cached")
    void shouldReturnEmptyWhen_CityNotFound() {
        assertThat(cacheManager.getActual("Paris")).isEmpty();
    }

    @Test
    @DisplayName("Should return empty and remove entry when item is expired")
    void shouldReturnEmptyWhen_EntryExpired() throws InterruptedException {
        cacheManager = new WeatherCacheManager(5, 50);
        cacheManager.put("Tokyo", SAMPLE_RESPONSE);

        Thread.sleep(80);

        Optional<WeatherResponse> result = cacheManager.getActual("Tokyo");
        assertThat(result).isEmpty();
        assertThat(cacheManager.size()).isZero();
    }

    @Test
    @DisplayName("Should overwrite existing entry when same key is used")
    void shouldOverwriteExistingEntryWhen_KeyIsSame() {
        cacheManager.put("London", SAMPLE_RESPONSE);

        WeatherResponse updated = new WeatherResponse(
                new Weather("Clear", "sunny"),
                new Temperature(300.0, 299.0),
                10000,
                new Wind(1.0),
                123456789L,
                new Sys(1600000000L, 1600030000L),
                3600,
                "London"
        );

        cacheManager.put("London", updated);

        Optional<WeatherResponse> result = cacheManager.getActual("London");
        assertThat(result).isPresent();
        assertThat(result.get().weather().main()).isEqualTo("Clear");
    }

    @Test
    @DisplayName("Should evict oldest entry when cache exceeds max size")
    void shouldEvictOldestEntryWhen_CacheExceedsMaxSize() {
        cacheManager.put("City1", SAMPLE_RESPONSE);
        cacheManager.put("City2", SAMPLE_RESPONSE);
        cacheManager.put("City3", SAMPLE_RESPONSE);
        cacheManager.put("City4", SAMPLE_RESPONSE);

        assertThat(cacheManager.size()).isEqualTo(3);
        assertThat(cacheManager.getCachedCities()).doesNotContain("city1");
    }

    @Test
    @DisplayName("Should remove only expired entries when evictExpired() is called")
    void shouldRemoveOnlyExpiredEntriesWhen_EvictExpiredCalled() throws InterruptedException {

        cacheManager = new WeatherCacheManager(5, 100);
        cacheManager.put("Active", SAMPLE_RESPONSE);
        cacheManager.put("SoonExpired", SAMPLE_RESPONSE);

        Thread.sleep(150);

        cacheManager.put("Fresh", SAMPLE_RESPONSE);
        cacheManager.evictExpired();

        Set<String> cities = cacheManager.getCachedCities();
        assertThat(cities).containsExactlyInAnyOrder("fresh");
    }

    @Test
    @DisplayName("Should remove all entries when clear() is called")
    void shouldRemoveAllEntriesWhen_ClearCalled() {
        cacheManager.put("London", SAMPLE_RESPONSE);
        cacheManager.put("Paris", SAMPLE_RESPONSE);

        cacheManager.clear();

        assertThat(cacheManager.size()).isZero();
        assertThat(cacheManager.getCachedCities()).isEmpty();
    }

    @Test
    @DisplayName("Should return unmodifiable set from getCachedCities()")
    void shouldReturnUnmodifiableSetWhen_GetCachedCitiesCalled() {
        cacheManager.put("London", SAMPLE_RESPONSE);
        Set<String> cities = cacheManager.getCachedCities();

        assertThatThrownBy(() -> cities.add("Berlin"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should treat city names as case-insensitive")
    void shouldTreatCityNamesAsCaseInsensitiveWhen_StoredAndRetrieved() {
        cacheManager.put("LoNdOn", SAMPLE_RESPONSE);

        Optional<WeatherResponse> lower = cacheManager.getActual("london");
        Optional<WeatherResponse> upper = cacheManager.getActual("LONDON");

        assertThat(lower).isPresent();
        assertThat(upper).isPresent();
    }

    @Test
    @DisplayName("Should update LRU order when entry is accessed")
    void shouldUpdateLruOrderWhen_EntryIsAccessed() {
        cacheManager.put("A", SAMPLE_RESPONSE);
        cacheManager.put("B", SAMPLE_RESPONSE);
        cacheManager.put("C", SAMPLE_RESPONSE);

        cacheManager.getActual("A");

        cacheManager.put("D", SAMPLE_RESPONSE);

        Set<String> cities = cacheManager.getCachedCities();
        assertThat(cities).containsExactlyInAnyOrder("a", "c", "d");
        assertThat(cities).doesNotContain("b");
    }
}
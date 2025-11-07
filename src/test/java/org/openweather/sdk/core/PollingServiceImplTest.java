package org.openweather.sdk.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openweather.sdk.api.OpenWeatherApi;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PollingServiceImpl}.
 * <p>
 * Verifies correct lifecycle management (start/shutdown), scheduling behavior,
 * error resilience, and interaction with cache and API.
 */
class PollingServiceImplTest {

    @Mock
    private OpenWeatherApi mockApiClient;

    @Mock
    private WeatherCacheManager mockCacheManager;

    @Mock
    private WeatherResponse mockWeatherResponse;

    private PollingServiceImpl pollingService;

    private static final Duration TEST_INTERVAL = Duration.ofMillis(100);

    private static final long ASYNC_WAIT_MS = 500;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pollingService = new PollingServiceImpl(mockApiClient, mockCacheManager, TEST_INTERVAL);
    }

    @AfterEach
    void tearDown() {
        if (pollingService != null) {
            pollingService.shutdown();
        }
    }

    @Test
    @DisplayName("Should throw exception when polling interval is invalid")
    void shouldThrowExceptionWhen_PollingIntervalIsInvalid() {
        assertThatThrownBy(() -> new PollingServiceImpl(mockApiClient, mockCacheManager, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PollingServiceImpl(mockApiClient, mockCacheManager, Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should allow multiple start calls without side effects (idempotent)")
    void shouldBeIdempotentWhen_StartCalledMultipleTimes() {
        pollingService.startPolling();
        pollingService.startPolling();
    }

    @Test
    @DisplayName("Should allow multiple shutdown calls without side effects (idempotent)")
    void shouldBeIdempotentWhen_ShutdownCalledMultipleTimes() {
        pollingService.startPolling();
        pollingService.shutdown();
        pollingService.shutdown();
    }

    @Test
    @DisplayName("Should stop scheduler gracefully when shutdown is called")
    void shouldStopSchedulerWhen_ShutdownCalled() {
        pollingService.startPolling();

        try {
            Thread.sleep(TEST_INTERVAL.toMillis() * 2);
        } catch (InterruptedException ignored) {}

        pollingService.shutdown();

        verifyNoMoreInteractions(mockApiClient);
    }

    @Test
    @DisplayName("Should update all cached cities successfully during polling")
    void shouldUpdateAllCachedCitiesWhen_PollingRunsSuccessfully() throws Exception {
        Set<String> cities = Set.of("CityA", "CityB");
        when(mockCacheManager.getCachedCities()).thenReturn(cities);

        when(mockApiClient.getWeather(anyString())).thenReturn(mockWeatherResponse);

        pollingService.startPolling();

        Thread.sleep(ASYNC_WAIT_MS);

        InOrder inOrder = inOrder(mockCacheManager, mockApiClient);
        inOrder.verify(mockCacheManager).evictExpired();
        inOrder.verify(mockCacheManager).getCachedCities();

        verify(mockApiClient, atLeastOnce()).getWeather("CityA");
        verify(mockApiClient, atLeastOnce()).getWeather("CityB");

        verify(mockCacheManager, atLeastOnce()).put("CityA", mockWeatherResponse);
        verify(mockCacheManager, atLeastOnce()).put("CityB", mockWeatherResponse);
    }

    @Test
    @DisplayName("Should continue processing when API throws an error for one city")
    void shouldContinueProcessingWhen_ApiThrowsErrorForSingleCity() throws Exception {
        Set<String> cities = Set.of("CityX", "CityY", "CityZ");
        when(mockCacheManager.getCachedCities()).thenReturn(cities);

        when(mockApiClient.getWeather("CityX")).thenReturn(mockWeatherResponse);
        when(mockApiClient.getWeather("CityY")).thenThrow(new WeatherApiException("API Rate Limit"));
        when(mockApiClient.getWeather("CityZ")).thenReturn(mockWeatherResponse);

        pollingService.startPolling();
        Thread.sleep(ASYNC_WAIT_MS);

        verify(mockApiClient, atLeastOnce()).getWeather("CityX");
        verify(mockApiClient, atLeastOnce()).getWeather("CityY");
        verify(mockApiClient, atLeastOnce()).getWeather("CityZ");

        verify(mockCacheManager, atLeastOnce()).put("CityX", mockWeatherResponse);
        verify(mockCacheManager, never()).put(eq("CityY"), any());
        verify(mockCacheManager, atLeastOnce()).put("CityZ", mockWeatherResponse);
    }

    @Test
    @DisplayName("Should skip update when cache is empty")
    void shouldSkipUpdateWhen_CacheIsEmpty() throws Exception {
        when(mockCacheManager.getCachedCities()).thenReturn(Set.of());

        pollingService.startPolling();
        Thread.sleep(ASYNC_WAIT_MS);

        verify(mockCacheManager, atLeastOnce()).evictExpired();
        verify(mockApiClient, never()).getWeather(anyString());
        verify(mockCacheManager, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Should stop scheduler and prevent further executions after shutdown")
    void shouldPreventFurtherExecutionsWhen_ShutdownCalled() throws Exception {
        when(mockCacheManager.getCachedCities()).thenReturn(Set.of("City"));

        pollingService.startPolling();
        Thread.sleep(150);

        pollingService.shutdown();

        reset(mockApiClient);

        Thread.sleep(300);

        verify(mockApiClient, never()).getWeather(anyString());
    }
}
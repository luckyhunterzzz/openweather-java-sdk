package org.openweather.sdk.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.model.SdkMode;
import org.openweather.sdk.model.WeatherResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WeatherSDKImpl}.
 * <p>
 * Verifies constructor validation, cache usage, and lifecycle handling
 * for SDK running in different modes.
 */
class WeatherSDKImplTest {

    @Mock
    WeatherCacheManager cacheManager;

    @Mock
    WeatherResponse mockResponse;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should throw exception when API key is empty")
    void shouldThrowExceptionWhen_ApiKeyIsEmpty() {
        assertThatThrownBy(() -> new WeatherSDKImpl("", SdkMode.ON_DEMAND, cacheManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when SDK mode is null")
    void shouldThrowExceptionWhen_ModeIsNull() {
        assertThatThrownBy(() -> new WeatherSDKImpl("key", null, cacheManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should return cached response when present in cache")
    void shouldReturnCachedResponseWhen_Present() throws Exception {
        when(cacheManager.getActual("London")).thenReturn(Optional.of(mockResponse));

        WeatherSDKImpl sdk = new WeatherSDKImpl("key", SdkMode.ON_DEMAND, cacheManager);

        WeatherResponse result = sdk.getCurrentWeather("London");

        assertThat(result).isSameAs(mockResponse);
        verify(cacheManager).getActual("London");
        verify(cacheManager, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Should shutdown gracefully in ON_DEMAND mode")
    void shouldShutdownGracefullyWhen_OnDemandMode() {
        WeatherSDKImpl sdk = new WeatherSDKImpl("key", SdkMode.ON_DEMAND, cacheManager);
        sdk.shutdown();
    }
}
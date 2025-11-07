package org.openweather.sdk.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openweather.sdk.cache.WeatherCacheManager;
import org.openweather.sdk.core.WeatherSDK;
import org.openweather.sdk.model.SdkMode;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link WeatherSDKFactory}.
 * <p>
 * These tests verify instance creation, validation rules, caching behavior,
 * and instance lifecycle management in the SDK factory.
 */
class WeatherSDKFactoryTest {

    private static final String API_KEY = "test-key";

    @AfterEach
    void tearDown() {
        WeatherSDKFactory.releaseSDK(API_KEY);
    }

    @Test
    @DisplayName("Should throw exception when API key is empty")
    void shouldThrowExceptionWhen_ApiKeyIsEmpty() {
        assertThatThrownBy(() -> WeatherSDKFactory.getOrCreateSDK(" ", SdkMode.ON_DEMAND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API Key cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when SdkMode is null")
    void shouldThrowExceptionWhen_ModeIsNull() {
        assertThatThrownBy(() -> WeatherSDKFactory.getOrCreateSDK(API_KEY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SdkMode cannot be null");
    }

    @Test
    @DisplayName("Should return same SDK instance for same API key")
    void shouldReturnSameInstanceWhen_SameApiKey() {
        WeatherSDK sdk1 = WeatherSDKFactory.getOrCreateSDK(API_KEY, SdkMode.ON_DEMAND);
        WeatherSDK sdk2 = WeatherSDKFactory.getOrCreateSDK(API_KEY, SdkMode.ON_DEMAND);

        assertThat(sdk1).isSameAs(sdk2);
        assertThat(WeatherSDKFactory.getActiveInstanceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create distinct instances for different API keys")
    void shouldReturnDifferentInstancesWhen_DifferentApiKeys() {
        WeatherSDK sdk1 = WeatherSDKFactory.getOrCreateSDK("key1", SdkMode.ON_DEMAND);
        WeatherSDK sdk2 = WeatherSDKFactory.getOrCreateSDK("key2", SdkMode.ON_DEMAND);

        assertThat(sdk1).isNotSameAs(sdk2);
        assertThat(WeatherSDKFactory.getActiveInstanceCount()).isEqualTo(2);

        WeatherSDKFactory.releaseSDK("key1");
        WeatherSDKFactory.releaseSDK("key2");
    }

    @Test
    @DisplayName("Should release SDK instance and return true when exists")
    void shouldReleaseSdkWhen_Exists() {
        WeatherSDKFactory.getOrCreateSDK(API_KEY, SdkMode.ON_DEMAND);

        boolean released = WeatherSDKFactory.releaseSDK(API_KEY);

        assertThat(released).isTrue();
        assertThat(WeatherSDKFactory.getActiveInstanceCount()).isZero();
    }

    @Test
    @DisplayName("Should return false when trying to release unknown API key")
    void shouldReturnFalseWhen_ReleasingUnknownInstance() {
        boolean result = WeatherSDKFactory.releaseSDK("unknown-key");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should use provided custom cache manager when creating SDK")
    void shouldUseCustomCacheManagerWhen_Provided() {
        WeatherCacheManager customCache = new WeatherCacheManager();
        WeatherSDK sdk = WeatherSDKFactory.getOrCreateSDK(API_KEY, SdkMode.ON_DEMAND, customCache, Duration.ofSeconds(5));

        assertThat(((org.openweather.sdk.core.WeatherSDKImpl) sdk).getCacheManager())
                .isSameAs(customCache);
    }
}
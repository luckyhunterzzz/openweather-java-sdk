package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The main data model returned by the SDK client.
 * Accurately matches the expected JSON response structure.
 */
public record WeatherResponse(
        @JsonProperty("weather")
        Weather weather,

        @JsonProperty("temperature")
        Temperature temperature,

        @JsonProperty("visibility")
        int visibility,

        @JsonProperty("wind")
        Wind wind,

        @JsonProperty("datetime")
        long datetime,

        @JsonProperty("sys")
        Sys sys,

        @JsonProperty("timezone")
        int timezone,

        @JsonProperty("name")
        String name
) {
}
package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nested model representing temperature data.
 * Corresponds to the "temperature" structure in the final SDK response.
 */
public record Temperature(
        @JsonProperty("temp")
        double temp,

        @JsonProperty("feels_like")
        double feelsLike
) {
}

package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nested model representing system-related data (sunrise and sunset).
 * Corresponds to the "sys" structure in the final SDK response.
 */
public record Sys(
        @JsonProperty("sunrise")
        long sunrise,

        @JsonProperty("sunset")
        long sunset
) {
}

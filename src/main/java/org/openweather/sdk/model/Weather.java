package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nested model representing general weather conditions.
 * Corresponds to the "weather" structure in the final SDK response.
 */
public record Weather(
        @JsonProperty("main")
        String main,

        @JsonProperty("description")
        String description
) {
}
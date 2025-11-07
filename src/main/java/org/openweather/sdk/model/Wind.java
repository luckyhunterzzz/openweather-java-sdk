package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nested model representing wind data.
 * Corresponds to the "wind" structure in the final SDK response.
 */
public record Wind(
        @JsonProperty("speed")
        double speed
) {
}

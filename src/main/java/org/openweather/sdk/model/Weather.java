package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Вложенная модель, описывающая общие условия погоды.
 * Соответствует структуре "weather" в конечном ответе SDK.
 */
public record Weather(
        /** Краткое описание погоды (например, "Clouds", "Clear"). */
        @JsonProperty("main")
        String main,

        /** Детальное описание погоды (например, "scattered clouds"). */
        @JsonProperty("description")
        String description
) {
}
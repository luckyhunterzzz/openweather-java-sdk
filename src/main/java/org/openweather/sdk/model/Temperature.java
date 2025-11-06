package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Вложенная модель, описывающая температурные данные.
 * Соответствует структуре "temperature" в конечном ответе SDK.
 */
public record Temperature(
        /** Текущая температура в Кельвинах. */
        @JsonProperty("temp")
        double temp,

        /** Температура "ощущается как" в Кельвинах. */
        @JsonProperty("feels_like")
        double feelsLike
) {
}

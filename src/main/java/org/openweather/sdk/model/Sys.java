package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Вложенная модель, описывающая системные данные (восход и закат).
 * Соответствует структуре "sys" в конечном ответе SDK.
 */
public record Sys(
        /** Время восхода солнца (UNIX time, UTC). */
        @JsonProperty("sunrise")
        long sunrise,

        /** Время заката солнца (UNIX time, UTC). */
        @JsonProperty("sunset")
        long sunset
) {
}

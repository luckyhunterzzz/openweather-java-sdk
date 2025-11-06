package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Вложенная модель, описывающая данные о ветре.
 * Соответствует структуре "wind" в конечном ответе SDK.
 */
public record Wind(
        /** Скорость ветра в метрах/секунду. */
        @JsonProperty("speed")
        double speed
) {
}

package org.openweather.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Главная модель данных, которую возвращает SDK клиенту.
 * Точно соответствует требуемой JSON структуре.
 */
public record WeatherResponse(
        /** Данные общих погодных условий. */
        @JsonProperty("weather")
        Weather weather,

        /** Данные о температуре. */
        @JsonProperty("temperature")
        Temperature temperature,

        /** Горизонтальная видимость в метрах. */
        @JsonProperty("visibility")
        int visibility,

        /** Данные о ветре. */
        @JsonProperty("wind")
        Wind wind,

        /** Время получения данных (UNIX time). */
        @JsonProperty("datetime")
        long datetime,

        /** Системные данные (восход/закат). */
        @JsonProperty("sys")
        Sys sys,

        /** Разница во времени UTC в секундах. */
        @JsonProperty("timezone")
        int timezone,

        /** Название города, по которому был сделан запрос. */
        @JsonProperty("name")
        String name
) {
}
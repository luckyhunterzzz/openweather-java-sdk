package org.openweather.sdk.exception;

/**
 * Исключение, выбрасываемое SDK в случае сбоя при работе с внешним
 * погодным API или внутренней ошибкой SDK.
 *
 * Является 'checked' исключением, чтобы пользователи SDK
 * были явно осведомлены о возможных проблемах.
 */
public class WeatherApiException extends Exception {
    /**
     * Конструктор с сообщением об ошибке.
     * @param message описание причины сбоя.
     */
    public WeatherApiException(String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и корневой причиной.
     * @param message описание причины сбоя.
     * @param cause корневое исключение (например, IOException от HTTP-клиента).
     */
    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
package org.openweather.sdk.exception;

/**
 * Exception thrown by the SDK in case of failure when interacting with the external
 * weather API or due to an internal SDK error.
 * <p>
 * This is a 'checked' exception, designed to ensure SDK users are explicitly
 * aware of potential API-related issues and are forced to handle them.
 */
public class WeatherApiException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.
     * @param message The detailed description of the failure.
     */
    public WeatherApiException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * @param message The detailed description of the failure.
     * @param cause The root cause exception (e.g., an IOException from the HTTP client).
     */
    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
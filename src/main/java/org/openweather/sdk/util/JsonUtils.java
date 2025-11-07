package org.openweather.sdk.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class that provides a centralized and reusable instance of
 * {@link ObjectMapper}.
 * <p>
 * {@code ObjectMapper} is thread-safe, and reusing a single instance is considered
 * a best practice for performance.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {}

    /**
     * Returns the singleton instance of the {@code ObjectMapper}.
     *
     * @return The thread-safe {@code ObjectMapper} instance.
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
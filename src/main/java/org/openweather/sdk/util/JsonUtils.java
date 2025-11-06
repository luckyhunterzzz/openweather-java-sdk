package org.openweather.sdk.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Утилитарный класс для предоставления централизованного и переиспользуемого
 * экземпляра ObjectMapper. ObjectMapper потокобезопасен и его
 * переиспользование является хорошей практикой.
 */
public final class JsonUtils {

    /** Статический и финальный экземпляр ObjectMapper. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Приватный конструктор, чтобы запретить создание экземпляров утилитарного класса. */
    private JsonUtils() {}

    /**
     * Возвращает единственный экземпляр ObjectMapper.
     * @return ObjectMapper.
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
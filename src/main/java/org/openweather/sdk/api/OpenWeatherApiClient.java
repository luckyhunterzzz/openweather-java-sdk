package org.openweather.sdk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;
import org.openweather.sdk.util.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Класс, отвечающий за прямое взаимодействие с OpenWeatherMap API.
 * Выполняет HTTP-запросы, обрабатывает коды состояния и парсит ответ.
 */
public class OpenWeatherApiClient implements OpenWeatherApi {

    private static final String API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String UNITS = "standard";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Инициализирует клиент API.
     * @param apiKey Ваш API ключ для OpenWeatherMap.
     */
    public OpenWeatherApiClient(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key cannot be null or empty.");
        }
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = JsonUtils.getMapper();
    }

    /**
     * Формирует полный URL для запроса погоды.
     * @param cityName Название города.
     * @return Полный URI для API-запроса.
     */
    private URI buildUri(String cityName) {
        String encodedCityName = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&appid=%s&units=%s", API_BASE_URL, encodedCityName, apiKey, UNITS);
        return URI.create(url);
    }

    /**
     * Обрабатывает HTTP код состояния и выбрасывает соответствующее исключение.
     * @param statusCode Код состояния HTTP.
     * @param uri URI, по которому был сделан запрос.
     * @throws WeatherApiException Исключение с описанием ошибки.
     */
    private void handleResponseStatus(int statusCode, URI uri) throws WeatherApiException {
        switch (statusCode) {
            case 401:
                throw new WeatherApiException("Unauthorized. Invalid API key or request format. Status Code: " + statusCode);
            case 404:
                throw new WeatherApiException("City not found or invalid request parameter. Status Code: " + statusCode);
            case 429:
                throw new WeatherApiException("Rate limit exceeded. Too many requests. Status Code: " + statusCode);
            case 500, 502, 503, 504:
                throw new WeatherApiException("Server error in OpenWeather API. Please try again later. Status Code: " + statusCode);
            case 200:
                break;
            default:
                throw new WeatherApiException("Unexpected error when calling API. Status Code: " + statusCode + ", URI: " + uri);
        }
    }

    /**
     * Конвертирует сырой JSON-ответ от OpenWeather API в унифицированную модель WeatherResponse SDK.
     * Здесь происходит маппинг полей.
     * @param jsonBody Сырая JSON-строка от API.
     * @return Объект WeatherResponse, готовый для передачи клиенту SDK.
     * @throws WeatherApiException Если произошла ошибка парсинга JSON.
     */
    private WeatherResponse parseJsonToResponse(String jsonBody) throws WeatherApiException {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);

            JsonNode weatherNode = root.path("weather").get(0);

            JsonNode mainNode = root.path("main");
            JsonNode windNode = root.path("wind");
            JsonNode sysNode = root.path("sys");

            return new WeatherResponse(
                    new org.openweather.sdk.model.Weather(
                            weatherNode.path("main").asText(),
                            weatherNode.path("description").asText()
                    ),
                    new org.openweather.sdk.model.Temperature(
                            mainNode.path("temp").asDouble(),
                            mainNode.path("feels_like").asDouble()
                    ),
                    root.path("visibility").asInt(),
                    new org.openweather.sdk.model.Wind(
                            windNode.path("speed").asDouble()
                    ),
                    root.path("dt").asLong(),
                    new org.openweather.sdk.model.Sys(
                            sysNode.path("sunrise").asLong(),
                            sysNode.path("sunset").asLong()
                    ),
                    root.path("timezone").asInt(),
                    root.path("name").asText()
            );
        } catch (Exception e) {
            throw new WeatherApiException("Failed to parse API response JSON into SDK model.", e);
        }
    }


    /**
     * {@inheritDoc}
     *
     * Выполняет синхронный запрос к OpenWeather API и возвращает обработанный ответ.
     *
     * @param cityName Название города.
     * @return Объект WeatherResponse.
     * @throws WeatherApiException В случае ошибки HTTP или ошибки API.
     */
    @Override
    public WeatherResponse getWeather(String cityName) throws WeatherApiException {
        URI uri = buildUri(cityName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            handleResponseStatus(response.statusCode(), uri);

            return parseJsonToResponse(response.body());

        } catch (IOException | InterruptedException e) {
            throw new WeatherApiException("Network error or request interrupted.", e);
        }
    }
}

package org.openweather.sdk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;
import org.openweather.sdk.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * The class responsible for direct interaction with the OpenWeatherMap API.
 * It executes HTTP requests, handles status codes, and parses the response.
 * Implements {@link OpenWeatherApi} public contract.
 */
public class OpenWeatherApiClient implements OpenWeatherApi {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherApiClient.class);

    private static final String API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String UNITS = "standard";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Initializes the API client using the default {@link HttpClient}.
     * @param apiKey Your OpenWeatherMap API key.
     * @throws IllegalArgumentException if the API key is null or empty.
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
     * Initializes the API client, allowing injection of a custom {@link HttpClient}
     * for better testing capabilities.
     *
     * @param apiKey Your OpenWeatherMap API key.
     * @param httpClient The custom HTTP client to use for requests.
     * @throws IllegalArgumentException if the API key is null or empty.
     */
    public OpenWeatherApiClient(String apiKey, HttpClient httpClient) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("API Key cannot be null or empty.");
            throw new IllegalArgumentException("API Key cannot be null or empty.");
        }
        this.apiKey = apiKey;
        this.httpClient = (httpClient != null) ? httpClient : HttpClient.newHttpClient();
        this.objectMapper = JsonUtils.getMapper();
    }

    /**
     * Constructs the full URI for the weather request.
     * @param cityName The name of the city.
     * @return The full URI for the API request.
     */
    protected URI buildUri(String cityName) {
        String encodedCityName = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&appid=%s&units=%s", API_BASE_URL, encodedCityName, apiKey, UNITS);
        return URI.create(url);
    }

    /**
     * Handles the HTTP status code and throws a corresponding exception if needed.
     * @param statusCode The HTTP status code.
     * @param uri The URI that was requested.
     * @throws WeatherApiException Exception describing the error.
     */
    private void handleResponseStatus(int statusCode, URI uri) throws WeatherApiException {
        if (statusCode == 200) {
            log.debug("API call successful (Status 200).");
            return;
        }

        String errorMessage = switch (statusCode) {
            case 401 -> "Unauthorized. Invalid API key or request format.";
            case 404 -> "City not found or invalid request parameter.";
            case 429 -> "Rate limit exceeded. Too many requests.";
            case 500, 502, 503, 504 -> "Server error in OpenWeather API. Please try again later.";
            default -> "Unexpected error when calling API.";
        };

        log.error("API Error: {} Status Code: {}. URI: {}", errorMessage, statusCode, uri);
        throw new WeatherApiException(errorMessage + " Status Code: " + statusCode);
    }

    /**
     * Converts the raw JSON response from the OpenWeather API into the unified SDK model {@link WeatherResponse}.
     *
     * @param jsonBody The raw JSON string from the API.
     * @return A {@link WeatherResponse} object ready to be passed to the SDK client.
     * @throws WeatherApiException If a JSON parsing error occurs.
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
            log.error("Failed to parse API response JSON into SDK model. Raw JSON: {}", jsonBody, e);
            throw new WeatherApiException("Failed to parse API response JSON into SDK model.", e);
        }
    }


    /**
     * {@inheritDoc}
     *
     * Executes a synchronous request to the OpenWeather API and returns the processed response.
     *
     * @param cityName The name of the city.
     * @return The {@link WeatherResponse} object.
     * @throws WeatherApiException In case of an HTTP error, API status error, or parsing error.
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

        } catch (IOException e) {
            log.error("Network error during API request for {}.", cityName, e);
            throw new WeatherApiException("Network error or connectivity issue.", e);
        } catch (InterruptedException e) {
            log.warn("API request for {} was interrupted.", cityName, e);
            throw new WeatherApiException("API request interrupted.", e);
        }
    }
}

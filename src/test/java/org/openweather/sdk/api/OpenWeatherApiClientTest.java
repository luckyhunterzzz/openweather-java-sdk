package org.openweather.sdk.api;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.model.WeatherResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenWeatherApiClient}.
 * <p>
 * Verifies constructor validation, correct JSON parsing,
 * error handling for various HTTP status codes, and URI encoding.
 */
class OpenWeatherApiClientTest {

    private OpenWeatherApiClient apiClient;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private static final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        apiClient = new OpenWeatherApiClient(API_KEY, mockHttpClient);
    }

    @Test
    @DisplayName("Should throw exception when API key is empty")
    void shouldThrowExceptionWhen_ApiKeyIsEmpty() {
        assertThatThrownBy(() -> new OpenWeatherApiClient(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API Key cannot be null or empty");
    }

    @Test
    @DisplayName("Should return parsed WeatherResponse when API returns valid JSON")
    void shouldReturnParsedWeatherResponseWhen_ApiReturnsValidJson() throws Exception {
        String json = """
                {
                  "weather": [{"main": "Clouds", "description": "scattered clouds"}],
                  "main": {"temp": 280.32, "feels_like": 278.12},
                  "visibility": 10000,
                  "wind": {"speed": 4.1},
                  "dt": 1605182400,
                  "sys": {"sunrise": 1605160800, "sunset": 1605193200},
                  "timezone": 3600,
                  "name": "London"
                }
                """;

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(json);

        WeatherResponse response = apiClient.getWeather("London");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("London");
        assertThat(response.weather().main()).isEqualTo("Clouds");
        assertThat(response.temperature().temp()).isEqualTo(280.32);
        assertThat(response.sys().sunrise()).isEqualTo(1605160800);
        assertThat(response.wind().speed()).isEqualTo(4.1);
    }

    @Test
    @DisplayName("Should throw WeatherApiException when status code is 401")
    void shouldThrowWeatherApiExceptionWhen_StatusCodeIs401() throws Exception {
        testStatusCodeThrows(401, "Unauthorized");
    }

    @Test
    @DisplayName("Should throw WeatherApiException when status code is 404")
    void shouldThrowWeatherApiExceptionWhen_StatusCodeIs404() throws Exception {
        testStatusCodeThrows(404, "City not found");
    }

    @Test
    @DisplayName("Should throw WeatherApiException when status code is 429")
    void shouldThrowWeatherApiExceptionWhen_StatusCodeIs429() throws Exception {
        testStatusCodeThrows(429, "Rate limit exceeded");
    }

    @Test
    @DisplayName("Should throw WeatherApiException when status code is 500")
    void shouldThrowWeatherApiExceptionWhen_StatusCodeIs500() throws Exception {
        testStatusCodeThrows(500, "Server error");
    }

    private void testStatusCodeThrows(int code, String expectedMessagePart) throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(code);
        when(mockHttpResponse.body()).thenReturn("{}");

        assertThatThrownBy(() -> apiClient.getWeather("Paris"))
                .isInstanceOf(WeatherApiException.class)
                .hasMessageContaining(expectedMessagePart);
    }

    @Test
    @DisplayName("Should throw WeatherApiException when JSON is invalid")
    void shouldThrowWeatherApiExceptionWhen_JsonIsInvalid() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("invalid_json");

        assertThatThrownBy(() -> apiClient.getWeather("Moscow"))
                .isInstanceOf(WeatherApiException.class)
                .hasMessageContaining("Failed to parse API response JSON");
    }

    @Test
    @DisplayName("Should throw WeatherApiException when network request fails")
    void shouldThrowWeatherApiExceptionWhen_NetworkFails() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection failed"));

        assertThatThrownBy(() -> apiClient.getWeather("Berlin"))
                .isInstanceOf(WeatherApiException.class)
                .hasMessageContaining("Network error or connectivity issue");
    }

    @Test
    @DisplayName("Should correctly encode city name in URI")
    void shouldEncodeCityNameCorrectlyWhen_BuildingUri() {
        OpenWeatherApiClient client = new OpenWeatherApiClient("key", mockHttpClient);
        URI uri = client.buildUri("São Paulo");
        assertThat(uri.toString()).contains("S%C3%A3o+Paulo");
    }
}
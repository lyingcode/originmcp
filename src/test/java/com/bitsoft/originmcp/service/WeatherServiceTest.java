package com.bitsoft.originmcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestClient restClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private ResponseSpec responseSpec;

    private WeatherService weatherService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        weatherService = new WeatherService();
        // Use reflection to inject the mock RestClient
        try {
            java.lang.reflect.Field field = WeatherService.class.getDeclaredField("restClient");
            field.setAccessible(true);
            field.set(weatherService, restClient);
        } catch (Exception e) {
            fail("Failed to inject mock RestClient: " + e.getMessage());
        }

        // Common mock setup with lenient() to avoid UnnecessaryStubbingException
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testGetWeather_Success() {
        // Arrange
        String mockResponse = """
            {
                "current": {
                    "temperature_2m": 15.2,
                    "relative_humidity_2m": 65,
                    "wind_speed_10m": 12.5,
                    "weather_code": 0
                }
            }
            """;

        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        // Act
        String result = weatherService.fetchWeatherData("shenzhen");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Current Weather for shenzhen"));
        assertTrue(result.contains("Temperature: 15.2°C"));
        assertTrue(result.contains("Humidity: 65%"));
        assertTrue(result.contains("Wind Speed: 12.5 km/h"));
        assertTrue(result.contains("Clear sky"));

        verify(restClient, times(1)).get();
    }

    @Test
    void testGetWeather_WithCoordinates() {
        // Arrange
        String mockResponse = """
            {
                "current": {
                    "temperature_2m": 20.0,
                    "relative_humidity_2m": 50,
                    "wind_speed_10m": 8.0,
                    "weather_code": 1
                }
            }
            """;

        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        // Act
        String result = weatherService.fetchWeatherData("22.5431,114.0579");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Temperature: 20.0°C"));
        assertTrue(result.contains("Mainly clear"));
    }

    @Test
    void testGetWeather_ApiException() {
        // Arrange
        when(responseSpec.body(String.class))
                .thenThrow(new RuntimeException("API connection failed"));

        // Act
        String result = weatherService.fetchWeatherData("beijing");

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("Error retrieving weather:"));
        assertTrue(result.contains("API connection failed"));
    }

    @Test
    void testGetForecast_Success() {
        // Arrange
        String mockResponse = """
            {
                "daily": {
                    "time": ["2026-01-03", "2026-01-04", "2026-01-05"],
                    "temperature_2m_max": [18.5, 20.1, 19.8],
                    "temperature_2m_min": [12.3, 13.7, 12.9],
                    "weather_code": [0, 1, 2]
                }
            }
            """;

        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        // Act
        String result = weatherService.getForecast("shanghai");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("7-Day Forecast for shanghai"));
        assertTrue(result.contains("2026-01-03"));
        assertTrue(result.contains("High: 18.5°C, Low: 12.3°C"));
        assertTrue(result.contains("Clear sky"));
    }

    @Test
    void testGetForecast_ApiException() {
        // Arrange
        when(responseSpec.body(String.class))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        String result = weatherService.getForecast("beijing");

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("Error retrieving forecast:"));
        assertTrue(result.contains("Network error"));
    }

    @Test
    void testGetCityCoordinates_KnownCities() {
        // Test that known cities work
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("beijing"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("shanghai"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("shenzhen"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("guangzhou"));
    }

    @Test
    void testGetCityCoordinates_UnknownCity() {
        // Act & Assert - should throw exception for unknown city
        String result = weatherService.fetchWeatherData("unknowncity");

        // Assert - should return error message about unknown city
        assertTrue(result.contains("Error retrieving weather:") &&
                   result.contains("Unknown city"));
    }

    @Test
    void testGetWeather_ChineseCity() {
        // Arrange
        String mockResponse = """
            {
                "current": {
                    "temperature_2m": 15.0,
                    "relative_humidity_2m": 52,
                    "wind_speed_10m": 7.6,
                    "weather_code": 0
                }
            }
            """;

        when(responseSpec.body(String.class)).thenReturn(mockResponse);

        // Act
        String result = weatherService.fetchWeatherData("深圳");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Temperature: 15.0°C"));
    }

    @Test
    void testWeatherDescription() {
        // Test different weather codes
        String mockResponseClear = """
            {"current": {"temperature_2m": 20, "weather_code": 0}}
            """;

        String mockResponseRain = """
            {"current": {"temperature_2m": 15, "weather_code": 61}}
            """;

        when(responseSpec.body(String.class))
                .thenReturn(mockResponseClear)
                .thenReturn(mockResponseRain);

        String resultClear = weatherService.fetchWeatherData("beijing");
        assertTrue(resultClear.contains("Clear sky"));

        String resultRain = weatherService.fetchWeatherData("beijing");
        assertTrue(resultRain.contains("Rain"));
    }
}

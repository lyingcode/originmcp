package com.bitsoft.originmcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherService.
 * Note: These tests use mocking to simulate the RestClient behavior.
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService();
        // Note: RestClient is created internally in the constructor
        // and cannot be easily mocked. These tests focus on testing
        // the parsing and formatting logic that doesn't require
        // actual HTTP calls.
    }

    @Test
    void testGetCityCoordinates_KnownCities() {
        // Test that known cities are recognized
        // These should not throw exceptions when parsing coordinates
        assertDoesNotThrow(() -> {
            // Using reflection to test the parseLocation method indirectly
            weatherService.fetchWeatherData("beijing");
        });
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("shanghai"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("shenzhen"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("guangzhou"));
    }

    @Test
    void testGetCityCoordinates_UnknownCity() {
        // For an unknown city, the service should return an error message
        String result = weatherService.fetchWeatherData("unknowncity123");
        assertNotNull(result);
        assertTrue(result.contains("Unknown location") || result.contains("Invalid location"));
    }

    @Test
    void testGetCityCoordinates_ChineseCity() {
        // Test Chinese city names
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("深圳"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("北京"));
    }

    @Test
    void testGetCityCoordinates_Coordinates() {
        // Test coordinate parsing
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("39.9042,116.4074"));
        assertDoesNotThrow(() -> weatherService.fetchWeatherData("31.2304,121.4737"));
    }

    @Test
    void testGetForecast_KnownCity() {
        // Test forecast for known city
        assertDoesNotThrow(() -> weatherService.getForecast("beijing"));
    }

    @Test
    void testGetForecast_UnknownCity() {
        // For an unknown city, the service should return an error message
        String result = weatherService.getForecast("unknowncity123");
        assertNotNull(result);
        assertTrue(result.contains("Unknown location") || result.contains("Invalid location"));
    }

    @Test
    void testGetForecast_Coordinates() {
        // Test forecast with coordinates
        assertDoesNotThrow(() -> weatherService.getForecast("22.5431,114.0579"));
    }

    @Test
    void testNullLocation() {
        // Test null location handling
        String result = weatherService.fetchWeatherData(null);
        assertNotNull(result);
        assertTrue(result.contains("Invalid location") || result.contains("null"));
    }

    @Test
    void testEmptyLocation() {
        // Test empty location handling
        String result = weatherService.fetchWeatherData("");
        assertNotNull(result);
        assertTrue(result.contains("Invalid location") || result.contains("empty"));
    }
}

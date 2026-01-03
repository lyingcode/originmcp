package com.bitsoft.originmcp.controller;

import com.bitsoft.originmcp.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void testGetCurrentWeather_Success() throws Exception {
        String expectedResponse = "Current Weather for shenzhen:\n\nTemperature: 15.0°C\nHumidity: 52%\n";

        when(weatherService.getWeather(anyString()))
                .thenReturn(expectedResponse);

        mockMvc.perform(get("/api/weather/current")
                        .param("location", "shenzhen"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void testGetCurrentWeather_MissingParameter() throws Exception {
        mockMvc.perform(get("/api/weather/current"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetForecast_Success() throws Exception {
        String expectedResponse = "7-Day Forecast for beijing:\n\n2026-01-03:\n  High: 18°C, Low: 12°C\n";

        when(weatherService.getForecast(anyString()))
                .thenReturn(expectedResponse);

        mockMvc.perform(get("/api/weather/forecast")
                        .param("location", "beijing"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void testGetForecast_WithCoordinates() throws Exception {
        String expectedResponse = "7-Day Forecast for 22.5431,114.0579:\n\n...";

        when(weatherService.getForecast("22.5431,114.0579"))
                .thenReturn(expectedResponse);

        mockMvc.perform(get("/api/weather/forecast")
                        .param("location", "22.5431,114.0579"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void testGetCurrentWeather_ErrorHandling() throws Exception {
        String errorResponse = "Error retrieving weather: API connection failed";

        when(weatherService.getWeather("invalidcity"))
                .thenReturn(errorResponse);

        mockMvc.perform(get("/api/weather/current")
                        .param("location", "invalidcity"))
                .andExpect(status().isOk())
                .andExpect(content().string(errorResponse));
    }
}

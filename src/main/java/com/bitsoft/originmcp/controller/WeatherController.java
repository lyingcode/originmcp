package com.bitsoft.originmcp.controller;

import com.bitsoft.originmcp.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    public String getCurrentWeather(@RequestParam String location) {
        return weatherService.getWeather(location);
    }

    @GetMapping("/forecast")
    public String getForecast(@RequestParam String location) {
        return weatherService.getForecast(location);
    }
}

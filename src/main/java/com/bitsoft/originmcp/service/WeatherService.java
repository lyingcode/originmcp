package com.bitsoft.originmcp.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    
    // API constants
    private static final String API_BASE_URL = "https://api.open-meteo.com/v1";
    private static final String API_PATH_FORECAST = "/forecast";
    
    // Weather parameters
    private static final String PARAM_LATITUDE = "latitude";
    private static final String PARAM_LONGITUDE = "longitude";
    private static final String PARAM_CURRENT = "current";
    private static final String PARAM_DAILY = "daily";
    private static final String PARAM_TIMEZONE = "timezone";
    
    // Current weather parameters
    private static final String CURRENT_TEMPERATURE_2M = "temperature_2m";
    private static final String CURRENT_RELATIVE_HUMIDITY_2M = "relative_humidity_2m";
    private static final String CURRENT_WIND_SPEED_10M = "wind_speed_10m";
    private static final String CURRENT_WEATHER_CODE = "weather_code";
    
    // Daily forecast parameters
    private static final String DAILY_TEMPERATURE_2M_MAX = "temperature_2m_max";
    private static final String DAILY_TEMPERATURE_2M_MIN = "temperature_2m_min";
    private static final String DAILY_WEATHER_CODE = "weather_code";
    private static final String DAILY_PRECIPITATION_SUM = "precipitation_sum";
    private static final String DAILY_WIND_SPEED_10M_MAX = "wind_speed_10m_max";
    
    // Coordinate validation ranges
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    
    // Coordinate pattern for parsing "lat,lon"
    private static final java.util.regex.Pattern COORDINATE_PATTERN =
        java.util.regex.Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");

    // Coordinate validation
    private static void validateCoordinates(double latitude, double longitude) {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException(
                String.format("Latitude %.4f is out of valid range [%.1f, %.1f]",
                    latitude, MIN_LATITUDE, MAX_LATITUDE));
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException(
                String.format("Longitude %.4f is out of valid range [%.1f, %.1f]",
                    longitude, MIN_LONGITUDE, MAX_LONGITUDE));
        }
    }

    // Coordinate record for type-safe representation
    private record Coordinates(double latitude, double longitude) {
        Coordinates {
            validateCoordinates(latitude, longitude);
        }
    }

    // City coordinates mapping
    private static final Map<String, Coordinates> CITY_COORDINATES = Map.ofEntries(
        Map.entry("beijing", new Coordinates(39.9042, 116.4074)),
        Map.entry("北京", new Coordinates(39.9042, 116.4074)),
        Map.entry("shanghai", new Coordinates(31.2304, 121.4737)),
        Map.entry("上海", new Coordinates(31.2304, 121.4737)),
        Map.entry("shenzhen", new Coordinates(22.5431, 114.0579)),
        Map.entry("深圳", new Coordinates(22.5431, 114.0579)),
        Map.entry("guangzhou", new Coordinates(23.1291, 113.2644)),
        Map.entry("广州", new Coordinates(23.1291, 113.2644)),
        Map.entry("new york", new Coordinates(40.7128, -74.0060)),
        Map.entry("nyc", new Coordinates(40.7128, -74.0060)),
        Map.entry("london", new Coordinates(51.5074, -0.1278)),
        Map.entry("tokyo", new Coordinates(35.6762, 139.6503)),
        Map.entry("paris", new Coordinates(48.8566, 2.3522)),
        Map.entry("los angeles", new Coordinates(34.0522, -118.2437)),
        Map.entry("la", new Coordinates(34.0522, -118.2437))
    );

    // JSON parser
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Parse location string into Coordinates
    private Coordinates parseLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            log.warn("Location parameter is null or empty");
            throw new IllegalArgumentException("Location cannot be null or empty");
        }
        String normalized = location.trim().toLowerCase(Locale.ROOT);
        
        // Check if location is a coordinate pair
        Matcher matcher = COORDINATE_PATTERN.matcher(location.trim());
        if (matcher.matches()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lon = Double.parseDouble(matcher.group(2));
                log.debug("Parsed coordinates: latitude={}, longitude={} from '{}'", lat, lon, location);
                return new Coordinates(lat, lon);
            } catch (NumberFormatException e) {
                log.warn("Invalid coordinate format: {}", location, e);
                throw new IllegalArgumentException("Invalid coordinate format: " + location, e);
            }
        }
        
        // Look up city in mapping
        Coordinates coords = CITY_COORDINATES.get(normalized);
        if (coords != null) {
            log.debug("Resolved city '{}' to coordinates: latitude={}, longitude={}", location, coords.latitude(), coords.longitude());
            return coords;
        }
        
        // If city not found, try to match with case-insensitive mapping
        // (already normalized, so just throw)
        log.warn("Unknown location: '{}'", location);
        throw new IllegalArgumentException(
            String.format("Unknown location: '%s'. Please use coordinates like '39.9042,116.4074' or a known city name.", location));
    }

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    @Tool(description = "Get current weather and forecast for a specific location by city name or coordinates")
    public String fetchWeatherData(String location) {
        log.info("Fetching weather data for location: {}", location);
        try {
            Coordinates coordinates = parseLocation(location);
            
            // Build URL using UriComponentsBuilder
            String weatherUrl = UriComponentsBuilder.fromPath(API_PATH_FORECAST)
                .queryParam(PARAM_LATITUDE, coordinates.latitude())
                .queryParam(PARAM_LONGITUDE, coordinates.longitude())
                .queryParam(PARAM_CURRENT, String.join(",",
                    CURRENT_TEMPERATURE_2M,
                    CURRENT_RELATIVE_HUMIDITY_2M,
                    CURRENT_WIND_SPEED_10M,
                    CURRENT_WEATHER_CODE))
                .queryParam(PARAM_DAILY, String.join(",",
                    DAILY_WEATHER_CODE,
                    DAILY_TEMPERATURE_2M_MAX,
                    DAILY_TEMPERATURE_2M_MIN))
                .queryParam(PARAM_TIMEZONE, "auto")
                .build()
                .toUriString();

            log.debug("Requesting weather API: {}", weatherUrl);
            String response = restClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .body(String.class);

            log.debug("Weather API response received");
            return formatWeatherResponse(response, location);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid location parameter: {}", location, e);
            return "Invalid location: " + e.getMessage();
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Error calling weather API for location: {}", location, e);
            return "Unable to retrieve weather data: " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error fetching weather for location: {}", location, e);
            return "Unexpected error retrieving weather: " + e.getMessage();
        }
    }

    @Tool(description = "Get detailed 7-day weather forecast for a location")
    public String getForecast(String location) {
        log.info("Fetching forecast for location: {}", location);
        try {
            Coordinates coordinates = parseLocation(location);
            
            // Build URL using UriComponentsBuilder
            String forecastUrl = UriComponentsBuilder.fromPath(API_PATH_FORECAST)
                .queryParam(PARAM_LATITUDE, coordinates.latitude())
                .queryParam(PARAM_LONGITUDE, coordinates.longitude())
                .queryParam(PARAM_DAILY, String.join(",",
                    DAILY_WEATHER_CODE,
                    DAILY_TEMPERATURE_2M_MAX,
                    DAILY_TEMPERATURE_2M_MIN,
                    DAILY_PRECIPITATION_SUM,
                    DAILY_WIND_SPEED_10M_MAX))
                .queryParam(PARAM_TIMEZONE, "auto")
                .build()
                .toUriString();

            log.debug("Requesting forecast API: {}", forecastUrl);
            String response = restClient.get()
                    .uri(forecastUrl)
                    .retrieve()
                    .body(String.class);

            log.debug("Forecast API response received");
            return formatForecastResponse(response, location);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid location parameter: {}", location, e);
            return "Invalid location: " + e.getMessage();
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Error calling forecast API for location: {}", location, e);
            return "Unable to retrieve forecast: " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error fetching forecast for location: {}", location, e);
            return "Unexpected error retrieving forecast: " + e.getMessage();
        }
    }


    private String formatWeatherResponse(String response, String location) {
        StringBuilder result = new StringBuilder();
        result.append("Current Weather for ").append(location).append(":\n\n");

        String temp = extractJsonValue(response, "temperature_2m", "current");
        String humidity = extractJsonValue(response, "relative_humidity_2m", "current");
        String windSpeed = extractJsonValue(response, "wind_speed_10m", "current");
        String weatherCode = extractJsonValue(response, "weather_code", "current");

        if (temp != null) {
            result.append("Temperature: ").append(temp).append("°C\n");
        }
        if (humidity != null) {
            result.append("Humidity: ").append(humidity).append("%\n");
        }
        if (windSpeed != null) {
            result.append("Wind Speed: ").append(windSpeed).append(" km/h\n");
        }
        if (weatherCode != null) {
            result.append("Conditions: ").append(getWeatherDescription(weatherCode)).append("\n");
        }

        return result.toString();
    }

    private String formatForecastResponse(String response, String location) {
        StringBuilder result = new StringBuilder();
        result.append("7-Day Forecast for ").append(location).append(":\n\n");

        // Extract daily data arrays
        String timeSection = extractArraySection(response, "time", "daily");
        String maxTempSection = extractArraySection(response, "temperature_2m_max", "daily");
        String minTempSection = extractArraySection(response, "temperature_2m_min", "daily");
        String weatherCodeSection = extractArraySection(response, "weather_code", "daily");

        if (timeSection != null && maxTempSection != null && minTempSection != null) {
            String[] dates = timeSection.split(",");
            String[] maxTemps = maxTempSection.split(",");
            String[] minTemps = minTempSection.split(",");
            String[] weatherCodes = weatherCodeSection != null ? weatherCodeSection.split(",") : new String[dates.length];

            int count = Math.min(7, Math.min(dates.length, Math.min(maxTemps.length, minTemps.length)));
            for (int i = 0; i < count; i++) {
                String date = dates[i].replaceAll("[\"\\[\\]]", "").trim();
                String maxTemp = maxTemps[i].replaceAll("[\"\\[\\]]", "").trim();
                String minTemp = minTemps[i].replaceAll("[\"\\[\\]]", "").trim();
                String code = i < weatherCodes.length ? weatherCodes[i].replaceAll("[\"\\[\\]]", "").trim() : "0";

                result.append(date).append(":\n");
                result.append("  High: ").append(maxTemp).append("°C, Low: ").append(minTemp).append("°C\n");
                result.append("  ").append(getWeatherDescription(code)).append("\n\n");
            }
        }

        return result.toString();
    }

    private String extractJsonValue(String json, String key, String section) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode sectionNode = root.path(section);
            if (sectionNode.isMissingNode()) {
                return null;
            }
            JsonNode valueNode = sectionNode.path(key);
            if (valueNode.isMissingNode() || valueNode.isNull()) {
                return null;
            }
            if (valueNode.isNumber()) {
                return String.valueOf(valueNode.asDouble());
            } else if (valueNode.isTextual()) {
                return valueNode.asText();
            } else {
                return valueNode.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to parse JSON for key {} in section {}", key, section, e);
            return null;
        }
    }

    private String extractArraySection(String json, String key, String section) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode sectionNode = root.path(section);
            if (sectionNode.isMissingNode()) {
                return null;
            }
            JsonNode arrayNode = sectionNode.path(key);
            if (!arrayNode.isArray()) {
                return null;
            }
            // Convert array to comma-separated string without brackets
            StringBuilder sb = new StringBuilder();
            for (JsonNode element : arrayNode) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                if (element.isNumber()) {
                    sb.append(element.asDouble());
                } else if (element.isTextual()) {
                    sb.append(element.asText());
                } else {
                    sb.append(element.toString());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Failed to parse JSON array for key {} in section {}", key, section, e);
            return null;
        }
    }

    private String getWeatherDescription(String code) {
        if (code == null) return "Unknown";

        return switch (code) {
            case "0" -> "Clear sky";
            case "1" -> "Mainly clear";
            case "2" -> "Partly cloudy";
            case "3" -> "Overcast";
            case "45", "48" -> "Foggy";
            case "51", "53", "55" -> "Drizzle";
            case "61", "63", "65" -> "Rain";
            case "71", "73", "75" -> "Snow";
            case "77" -> "Snow grains";
            case "80", "81", "82" -> "Rain showers";
            case "85", "86" -> "Snow showers";
            case "95" -> "Thunderstorm";
            case "96", "99" -> "Thunderstorm with hail";
            default -> "Weather code: " + code;
        };
    }
}

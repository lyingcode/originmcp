package com.bitsoft.originmcp.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com/v1")
                .build();
    }

    @Tool(description = "Get current weather and forecast for a specific location by city name or coordinates")
    public String getWeather(String location) {
        try {
            // Parse location (could be city name or "lat,lon")
            double latitude;
            double longitude;

            if (location.contains(",")) {
                String[] parts = location.split(",");
                latitude = Double.parseDouble(parts[0].trim());
                longitude = Double.parseDouble(parts[1].trim());
            } else {
                // For simplicity, support some common cities
                double[] coords = getCityCoordinates(location);
                latitude = coords[0];
                longitude = coords[1];
            }

            String weatherUrl = String.format(
                "/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto",
                latitude, longitude
            );

            String response = restClient.get()
                    .uri(weatherUrl)
                    .retrieve()
                    .body(String.class);

            return formatWeatherResponse(response, location);
        } catch (Exception e) {
            return "Error retrieving weather: " + e.getMessage();
        }
    }

    @Tool(description = "Get detailed 7-day weather forecast for a location")
    public String getForecast(String location) {
        try {
            double latitude;
            double longitude;

            if (location.contains(",")) {
                String[] parts = location.split(",");
                latitude = Double.parseDouble(parts[0].trim());
                longitude = Double.parseDouble(parts[1].trim());
            } else {
                double[] coords = getCityCoordinates(location);
                latitude = coords[0];
                longitude = coords[1];
            }

            String forecastUrl = String.format(
                "/forecast?latitude=%.4f&longitude=%.4f&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max&timezone=auto",
                latitude, longitude
            );

            String response = restClient.get()
                    .uri(forecastUrl)
                    .retrieve()
                    .body(String.class);

            return formatForecastResponse(response, location);
        } catch (Exception e) {
            return "Error retrieving forecast: " + e.getMessage();
        }
    }

    private double[] getCityCoordinates(String city) {
        // Common cities mapping
        return switch (city.toLowerCase().trim()) {
            case "beijing", "北京" -> new double[]{39.9042, 116.4074};
            case "shanghai", "上海" -> new double[]{31.2304, 121.4737};
            case "shenzhen", "深圳" -> new double[]{22.5431, 114.0579};
            case "guangzhou", "广州" -> new double[]{23.1291, 113.2644};
            case "new york", "nyc" -> new double[]{40.7128, -74.0060};
            case "london" -> new double[]{51.5074, -0.1278};
            case "tokyo" -> new double[]{35.6762, 139.6503};
            case "paris" -> new double[]{48.8566, 2.3522};
            case "los angeles", "la" -> new double[]{34.0522, -118.2437};
            default -> throw new IllegalArgumentException("Unknown city: " + city + ". Please use coordinates like '39.9042,116.4074'");
        };
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
        String searchPattern = "\"" + section + "\"";
        int sectionIndex = json.indexOf(searchPattern);
        if (sectionIndex == -1) return null;

        String sectionContent = json.substring(sectionIndex);
        int nextSection = sectionContent.indexOf("},", 100);
        if (nextSection > 0) {
            sectionContent = sectionContent.substring(0, nextSection);
        }

        String searchKey = "\"" + key + "\":";
        int keyIndex = sectionContent.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStart = keyIndex + searchKey.length();
        while (valueStart < sectionContent.length() && Character.isWhitespace(sectionContent.charAt(valueStart))) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < sectionContent.length() &&
               (Character.isDigit(sectionContent.charAt(valueEnd)) ||
                sectionContent.charAt(valueEnd) == '.' ||
                sectionContent.charAt(valueEnd) == '-')) {
            valueEnd++;
        }

        if (valueEnd > valueStart) {
            return sectionContent.substring(valueStart, valueEnd);
        }
        return null;
    }

    private String extractArraySection(String json, String key, String section) {
        String searchPattern = "\"" + section + "\"";
        int sectionIndex = json.indexOf(searchPattern);
        if (sectionIndex == -1) return null;

        String sectionContent = json.substring(sectionIndex);
        String searchKey = "\"" + key + "\":";
        int keyIndex = sectionContent.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int arrayStart = sectionContent.indexOf("[", keyIndex);
        if (arrayStart == -1) return null;

        int arrayEnd = sectionContent.indexOf("]", arrayStart);
        if (arrayEnd == -1) return null;

        return sectionContent.substring(arrayStart + 1, arrayEnd);
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

package com.jatin.weather_api.service;

import com.jatin.weather_api.dto.WeatherResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.jatin.weather_api.exception.WeatherServiceException;
import com.jatin.weather_api.exception.CityNotFoundException;

import java.util.concurrent.TimeUnit;

@Service
public class WeatherService {

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.cache.ttl-seconds}")
    private long ttlSeconds;

    public WeatherService(@Value("${weather.api.url}") String baseUrl, StringRedisTemplate redisTemplate) {
        this.restClient = RestClient.create(baseUrl);
        this.redisTemplate = redisTemplate;
    }

    public WeatherResponse getWeather(String city) {
        String cacheKey = city.toLowerCase();

        // 1. Check cache first
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, WeatherResponse.class);
            } catch (Exception e) {
                throw new WeatherServiceException("Failed to parse cached weather data for " + city, e);
            }
        }

        // 2. Cache miss - call the real API
        String rawJson = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{city}/today")
                        .queryParam("unitGroup", "metric")
                        .queryParam("key", apiKey)
                        .queryParam("contentType", "json")
                        .queryParam("include", "current")
                        .build(city))
                .retrieve()
                .onStatus(status -> status.value() == 400, (request, response) -> {
                    throw new CityNotFoundException(city);
                })
                .body(String.class);

        WeatherResponse weatherResponse;
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode current = root.get("currentConditions");

            weatherResponse = new WeatherResponse(
                    root.get("resolvedAddress").asText(),
                    current.get("temp").asDouble(),
                    current.get("feelslike").asDouble(),
                    current.get("humidity").asDouble(),
                    current.get("conditions").asText(),
                    current.get("windspeed").asDouble()
            );
        } catch (Exception e) {
            throw new WeatherServiceException("Failed to parse weather data for " + city, e);
        }

        // 3. Save to cache with TTL before returning
        try {
            String jsonToCache = objectMapper.writeValueAsString(weatherResponse);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new WeatherServiceException("Failed to cache weather data for " + city, e);
        }

        return weatherResponse;
    }
}
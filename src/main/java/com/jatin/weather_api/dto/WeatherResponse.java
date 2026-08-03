package com.jatin.weather_api.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private String city;
    private double temperature;
    private double feelsLike;
    private double humidity;
    private String condition;
    private double windSpeed;
}
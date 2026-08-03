package com.jatin.weather_api.exception;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String city) {
        super("Could not find weather data for city: " + city);
    }
}
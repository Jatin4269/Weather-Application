# Weather API

A Spring Boot REST API that fetches live weather data from a 3rd-party provider, with Redis caching, per-IP rate limiting, and centralized error handling. Includes a small animated frontend that reacts to current weather conditions.

**Live app:** https://weather-application-6xyh.onrender.com

## Features

- Live weather data via [Visual Crossing](https://www.visualcrossing.com/weather-api)
- Redis caching (cache-aside pattern, 5-minute TTL) to avoid redundant calls to the external API
- Per-IP rate limiting using Bucket4j (10 requests/minute)
- Centralized error handling via `@ControllerAdvice` — clean JSON errors for invalid cities or upstream failures
- Own DTO layer, decoupled from the 3rd-party provider's response shape
- Animated frontend (vanilla JS + CSS) that changes scene based on live conditions — sun and birds, rain and clouds, lightning, or wind
- Dockerized and deployed on Render, with hosted Redis on Upstash

## Tech Stack

- **Language/Framework:** Java 21, Spring Boot 4
- **HTTP Client:** Spring `RestClient`
- **Caching:** Redis (Spring Data Redis / Lettuce), hosted on Upstash in production
- **Rate Limiting:** Bucket4j
- **JSON Parsing:** Jackson
- **Frontend:** Thymeleaf, vanilla JS, CSS animations
- **Deployment:** Docker, Render

## API

```
GET /weather?city={cityName}
```

**Example response:**
```json
{
  "city": "jaipur",
  "temperature": 29.0,
  "feelsLike": 34.4,
  "humidity": 79.1,
  "condition": "Partially cloudy",
  "windSpeed": 12.5
}
```

**Error response (invalid city):**
```json
{
  "error": "Could not find weather data for city: asdkfjasdkf"
}
```

## Architecture

1. Client requests `/weather?city=jaipur`
2. Service checks Redis for a cached result (cache-aside pattern)
3. **Cache hit** — returns immediately, no external call
4. **Cache miss** — calls Visual Crossing's API, parses the response into an internal DTO, caches it with a TTL, then returns it
5. A servlet filter enforces per-IP rate limiting on all requests
6. A global exception handler catches invalid input or upstream failures and returns clean JSON errors instead of raw stack traces

## Running Locally

Requires Java 21, Maven, and a local Redis instance (e.g. via Docker: `docker run -d -p 6379:6379 redis`).

Set the following environment variables:
```
WEATHER_API_KEY=your_visual_crossing_api_key
```

Then run:
```bash
./mvnw spring-boot:run
```

The app will be available at `http://localhost:8080`.

## Key Design Decisions

- **Cache-aside over write-through:** weather data is read-heavy and doesn't need pre-warming; check-then-fetch is simpler and sufficient.
- **Own DTO instead of passing through raw JSON:** decouples the API's public contract from the 3rd-party provider's response shape, and trims the payload down to only what's needed.
- **Short TTL (5 minutes):** weather data goes stale quickly; a longer cache risks serving outdated conditions during fast-changing weather.
- **`RestClient` over `RestTemplate`:** `RestTemplate` has been in maintenance mode since Spring 5; `RestClient` is the current recommended synchronous HTTP client.

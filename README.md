# 🌦️ OpenWeather Java SDK

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Maven Central](https://img.shields.io/maven-central/v/org.openweather/openweather-java-sdk.svg)](https://search.maven.org/artifact/org.openweather/openweather-java-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Tests](https://img.shields.io/badge/tests-100%25-green)](#testing)

A **robust, thread-safe, and developer-friendly** Java SDK for accessing the [OpenWeatherMap Current Weather API](https://openweathermap.org/current).  
Designed for reliability and performance, featuring **built-in caching (TTL + LRU)**, **configurable polling**, and **SLF4J-based logging**.

## ⚡ Quickstart

```java
WeatherSDK sdk = WeatherSDKFactory.getOrCreateSDK("YOUR_API_KEY", SdkMode.ON_DEMAND);
WeatherResponse r = sdk.getCurrentWeather("London");
System.out.println(r.temperature().temp() + " K");
```
Example output:
```
London Temperature: 289.32 K
```

Example JSON response (mapped to WeatherResponse):
```
{
  "weather": {
    "main": "Clouds",
    "description": "overcast clouds"
  },
  "temperature": {
    "temp": 289.32,
    "feels_like": 288.71
  },
  "visibility": 10000,
  "wind": {
    "speed": 3.6
  },
  "datetime": 1730872335,
  "sys": {
    "sunrise": 1730860202,
    "sunset": 1730893411
  },
  "timezone": 0,
  "name": "London"
}
```
---

## 🚀 Features

✅ **Two Modes:**  
- `ON_DEMAND` — standard request/response mode.  
- `POLLING` — automatic background updates for cached cities.

✅ **Caching:**  
- Thread-safe, in-memory cache with configurable TTL (Time-To-Live) and LRU eviction policy.

✅ **Thread Safety:**  
- Achieved via `ReadWriteLock` in cache operations and a thread-safe factory for SDK instances.

✅ **Decoupled Logging:**  
- Uses **SLF4J**, allowing full flexibility for choosing your preferred backend (Logback, Log4j2, etc.).

✅ **Single-Key Singleton:**  
- Factory pattern ensures only one `WeatherSDK` instance exists per unique API key.

---

## 🛠️ Setup & Installation

### Dependencies

Requires **Java 17+**.

Key libraries:
- `jackson-databind` — JSON serialization/deserialization  
- `java.net.http.HttpClient` — built-in (Java 11+) HTTP client  
- `slf4j-api` — logging abstraction layer  

### Maven Configuration

Add to your project:

```xml
<dependency>
  <groupId>org.openweather</groupId>
  <artifactId>openweather-java-sdk</artifactId>
  <version>1.0-SNAPSHOT</version> 
</dependency>
```

> **Note:** If you build the SDK itself, include only `slf4j-api`.  
> Do **not** bundle logging implementations (like Logback) inside the SDK.

---

## 💡 Usage Examples

The SDK is managed via `WeatherSDKFactory`, ensuring singleton instances per API key.

---

### 1️⃣ Simple On-Demand Mode  
*(Recommended for lightweight or single-user applications)*

The SDK only calls the API if data for a city is missing or expired in the cache.

```java
import org.openweather.sdk.core.WeatherSDK;
import org.openweather.sdk.exception.WeatherApiException;
import org.openweather.sdk.factory.WeatherSDKFactory;
import org.openweather.sdk.model.SdkMode;
import org.openweather.sdk.model.WeatherResponse;

final String MY_API_KEY = "YOUR_OPENWEATHER_API_KEY";

// Create or get SDK instance
WeatherSDK sdk = WeatherSDKFactory.getOrCreateSDK(MY_API_KEY, SdkMode.ON_DEMAND);

try {
    WeatherResponse moscow = sdk.getCurrentWeather("Moscow");
    System.out.println("Moscow Temperature: " + moscow.temperature().temp() + " K");

    // Subsequent calls within TTL are served instantly from cache
    WeatherResponse cached = sdk.getCurrentWeather("Moscow");

} catch (WeatherApiException e) {
    System.err.println("API Error: " + e.getMessage());
} finally {
    // Recommended cleanup
    WeatherSDKFactory.releaseSDK(MY_API_KEY);
}
```

---

### 2️⃣ Polling Mode  
*(Recommended for background or multi-client systems)*

The SDK spawns a background thread that periodically refreshes cached data.

```java
import org.openweather.sdk.core.WeatherSDK;
import org.openweather.sdk.factory.WeatherSDKFactory;
import org.openweather.sdk.model.SdkMode;
import org.openweather.sdk.cache.WeatherCacheManager;
import java.time.Duration;

// Custom configuration
final Duration customPollingInterval = Duration.ofMinutes(5);
final WeatherCacheManager customCache = new WeatherCacheManager(20, Duration.ofMinutes(15).toMillis());

// Create SDK in POLLING mode
WeatherSDK sdk = WeatherSDKFactory.getOrCreateSDK(
    "YOUR_API_KEY",
    SdkMode.POLLING,
    customCache,
    customPollingInterval
);

sdk.getCurrentWeather("London");
sdk.getCurrentWeather("Tokyo");

// Background thread now updates these cities every 5 minutes

// Graceful shutdown
WeatherSDKFactory.releaseSDK("YOUR_API_KEY");
```

---

## 🛑 Cleanup (Releasing SDK Instances)

Always release instances in `POLLING` mode to stop background threads gracefully:

```java
boolean released = WeatherSDKFactory.releaseSDK(MY_API_KEY);
if (released) {
    System.out.println("SDK and background services successfully shut down.");
}
```

---

## ⚙️ Logging Configuration

The SDK uses **SLF4J** for logging.

Add a logging backend to see output (e.g., Logback):

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.6</version>
</dependency>
```

You can fully customize the log level, pattern, and output destination.

Example for `logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

---

## 🧪 Testing

The project includes unit tests for:
- API client (`OpenWeatherApiClientTest`)
- Cache (`WeatherCacheManagerTest`)
- SDK core (`WeatherSDKImplTest`)
- Polling service (`PollingServiceImplTest`)
- Factory (`WeatherSDKFactoryTest`)

Run tests with:
```bash
mvn clean test
```

---

## 🧩 License

Distributed under the MIT License.  
See `LICENSE` file for more details.

---

💬 **Author:** *Airat Zaripov*  
📦 **Version:** `1.0.0`  
🔗 **Repository:** [https://github.com/luckyhunterzzz/openweather-java-sdk](https://github.com/luckyhunterzzz/openweather-java-sdk)

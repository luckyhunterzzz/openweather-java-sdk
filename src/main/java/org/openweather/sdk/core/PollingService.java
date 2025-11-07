package org.openweather.sdk.core;

/**
 * Contract for a service responsible for background management and periodic
 * update (Polling Mode) of cached weather data.
 */
public interface PollingService {

    /**
     * Starts the periodic background task to poll the API and update cached data.
     * This operation should be idempotent (calling it multiple times should only
     * start the scheduler once).
     */
    void startPolling();

    /**
     * Initiates a controlled and graceful shutdown of the polling scheduler
     * and associated background threads. Implementations should attempt to
     * terminate gracefully before resorting to forceful shutdown.
     */
    void shutdown();
}
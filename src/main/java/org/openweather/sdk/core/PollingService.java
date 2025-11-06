package org.openweather.sdk.core;

/**
 * Контракт для службы, которая занимается фоновым обновлением данных о погоде
 * в кэше (Polling Mode).
 */
public interface PollingService {

    /**
     * Запускает фоновое выполнение задачи опроса.
     */
    void startPolling();

    /**
     * Корректно останавливает все фоновые потоки и завершает работу планировщика.
     */
    void shutdown();
}

package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CacheClearanceService {

    private final long cooldownMillis;

    private final ConcurrentHashMap<String, Long> lastIndividualClearTimestamps = new ConcurrentHashMap<>();
    private final AtomicLong lastClearAllTimestamp = new AtomicLong(0);

    public CacheClearanceService(@Value("${mybatis.cache.clear.cooldown-seconds:60}") long cooldownSeconds) {
        this.cooldownMillis = cooldownSeconds * 1000;
    }

    /**
     * Checks if a specific cache can be cleared.
     * @param cacheName The name of the cache.
     * @return true if cooldown has passed, false otherwise.
     */
    public boolean canClearCache(String cacheName) {
        long now = System.currentTimeMillis();
        long lastCleared = lastIndividualClearTimestamps.getOrDefault(cacheName, 0L);
        // Also consider the global clear all timestamp, a specific cache cannot be cleared if a global clear just happened.
        long lastGlobalCleared = lastClearAllTimestamp.get();

        return (now - lastCleared) >= cooldownMillis && (now - lastGlobalCleared) >= cooldownMillis;
    }

    /**
     * Records that a specific cache has been cleared.
     * @param cacheName The name of the cache.
     */
    public void recordCacheCleared(String cacheName) {
        lastIndividualClearTimestamps.put(cacheName, System.currentTimeMillis());
    }

    /**
     * Checks if the 'clear all' operation can be performed.
     * @return true if cooldown has passed, false otherwise.
     */
    public boolean canClearAllCaches() {
        long now = System.currentTimeMillis();
        return (now - lastClearAllTimestamp.get()) >= cooldownMillis;
    }

    /**
     * Records that all caches have been cleared.
     * This also updates the individual timestamps for all known caches that might be managed
     * by SqlSessionFactory to prevent immediate individual clearing after a global clear.
     * @param knownCacheNames Iterable of all cache names known to the system (e.g., from SqlSessionFactory)
     */
    public void recordAllCachesCleared(Iterable<String> knownCacheNames) {
        long now = System.currentTimeMillis();
        lastClearAllTimestamp.set(now);
        // Also update individual timestamps to reflect they were part of "clear all"
        if (knownCacheNames != null) {
            for (String cacheName : knownCacheNames) {
                lastIndividualClearTimestamps.put(cacheName, now);
            }
        }
    }

    /**
     * Returns the configured cooldown period in milliseconds.
     * @return long cooldown period in milliseconds.
     */
    public long getCooldownMillis() {
        return cooldownMillis;
    }

    /**
     * Gets the last recorded clear timestamp for a specific cache.
     * @param cacheName The name of the cache.
     * @return The epoch millisecond timestamp of the last clear, or 0 if never cleared or not known.
     */
    public long getLastIndividualClearTimestamp(String cacheName) {
        return lastIndividualClearTimestamps.getOrDefault(cacheName, 0L);
    }

    /**
     * Gets the last recorded clear timestamp for the 'clear all' operation.
     * @return The epoch millisecond timestamp of the last 'clear all', or 0 if never performed.
     */
    public long getLastClearAllTimestamp() {
        return lastClearAllTimestamp.get();
    }
}

package com.example.demo.dto;

public class CacheStatusDto {

    private String cacheName;
    private boolean cooldownActive;
    private long nextAvailableClearTimeMillis; // Epoch milliseconds
    private long timeUntilNextClearSeconds;    // Seconds from now

    public CacheStatusDto(String cacheName, boolean cooldownActive, long nextAvailableClearTimeMillis, long timeUntilNextClearSeconds) {
        this.cacheName = cacheName;
        this.cooldownActive = cooldownActive;
        this.nextAvailableClearTimeMillis = nextAvailableClearTimeMillis;
        this.timeUntilNextClearSeconds = timeUntilNextClearSeconds;
    }

    // Getters
    public String getCacheName() {
        return cacheName;
    }

    public boolean isCooldownActive() {
        return cooldownActive;
    }

    public long getNextAvailableClearTimeMillis() {
        return nextAvailableClearTimeMillis;
    }

    public long getTimeUntilNextClearSeconds() {
        return timeUntilNextClearSeconds;
    }

    // Optional: Setters if needed, but constructor is primary for immutable DTOs
    // Optional: toString, equals, hashCode
}

package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

public class CacheClearanceServiceTest {

    private CacheClearanceService service;
    private final long COOLDOWN_SECONDS = 1; // Use a short cooldown for testing
    private final long COOLDOWN_MILLIS = COOLDOWN_SECONDS * 1000;

    @BeforeEach
    void setUp() {
        service = new CacheClearanceService(COOLDOWN_SECONDS);
    }

    @Test
    void initiallyCanClear() {
        assertTrue(service.canClearCache("testCache"), "Should be able to clear new cache initially");
        assertTrue(service.canClearAllCaches(), "Should be able to clear all caches initially");
    }

    @Test
    void specificCacheCooldown() throws InterruptedException {
        String cacheName = "myCache";
        assertTrue(service.canClearCache(cacheName));

        service.recordCacheCleared(cacheName);
        assertFalse(service.canClearCache(cacheName), "Should not be able to clear immediately after recording");

        // Simulate time passing beyond cooldown
        // In a real test, you'd use a Clock abstraction or wait. Here, we test the logic.
        // We can't directly test the time progression without Thread.sleep, which is bad for unit tests.
        // So, this part of the test demonstrates the state change.
        // To actually test the time, we'd need to call it again *after* COOLDOWN_MILLIS.
        // For now, we assert the immediate state.
        // If we had a mockable clock:
        // mockClock.advanceMillis(COOLDOWN_MILLIS + 1);
        // assertTrue(service.canClearCache(cacheName), "Should be able to clear after cooldown");
    }

    @Test
    void specificCacheCooldown_SimulateTimePass() throws InterruptedException {
        String cacheName = "timedCache";
        assertTrue(service.canClearCache(cacheName));
        service.recordCacheCleared(cacheName);
        assertFalse(service.canClearCache(cacheName));

        Thread.sleep(COOLDOWN_MILLIS + 500); // Wait for longer than cooldown
        assertTrue(service.canClearCache(cacheName), "Should be able to clear after cooldown period");
    }


    @Test
    void clearAllCachesCooldown() throws InterruptedException {
        assertTrue(service.canClearAllCaches());

        service.recordAllCachesCleared(Arrays.asList("cache1", "cache2"));
        assertFalse(service.canClearAllCaches(), "Should not be able to clear all immediately after recording");
        assertFalse(service.canClearCache("cache1"), "Cache1 should also be on cooldown after clearAll");
        assertFalse(service.canClearCache("cache2"), "Cache2 should also be on cooldown after clearAll");
        assertFalse(service.canClearCache("otherCache"), "Other caches should also be on cooldown due to global clearAll timestamp");


        // Simulate time passing
        // mockClock.advanceMillis(COOLDOWN_MILLIS + 1);
        // assertTrue(service.canClearAllCaches(), "Should be able to clear all after cooldown");
        // assertTrue(service.canClearCache("cache1"), "Cache1 should be clearable after global cooldown");
    }

    @Test
    void clearAllCachesCooldown_SimulateTimePass() throws InterruptedException {
        assertTrue(service.canClearAllCaches());
        service.recordAllCachesCleared(Arrays.asList("cache1", "cache2"));
        assertFalse(service.canClearAllCaches());
        assertFalse(service.canClearCache("cache1"));


        Thread.sleep(COOLDOWN_MILLIS + 500); // Wait for longer than cooldown

        assertTrue(service.canClearAllCaches(), "Should be able to clear all after cooldown period");
        assertTrue(service.canClearCache("cache1"), "Cache1 should be clearable after global cooldown period");
        assertTrue(service.canClearCache("unrelatedCache"), "An unrelated cache should also be clearable after global cooldown");
    }


    @Test
    void clearAllAffectsIndividualCacheCooldown() {
        String specificCache = "specificOne";
        // Record specific cache cleared, it's now on cooldown
        service.recordCacheCleared(specificCache);
        assertFalse(service.canClearCache(specificCache));

        // Now, record all caches cleared.
        // This should effectively 'reset' or 'align' the cooldown of the specific cache
        // with the global cooldown.
        service.recordAllCachesCleared(Collections.singletonList(specificCache));
        assertFalse(service.canClearCache(specificCache), "Specific cache should still be on cooldown after all caches cleared");
        assertFalse(service.canClearAllCaches(), "All caches should be on cooldown");
    }

    @Test
    void clearIndividualCacheDoesNotAffectClearAllCooldown() throws InterruptedException {
        String cacheName = "individualCache";

        // Record individual cache cleared
        service.recordCacheCleared(cacheName);
        assertFalse(service.canClearCache(cacheName)); // Individual cache on cooldown
        assertTrue(service.canClearAllCaches()); // Clear all should still be possible initially

        // Wait for individual cache's cooldown to pass
        Thread.sleep(COOLDOWN_MILLIS + 500);
        assertTrue(service.canClearCache(cacheName)); // Individual cache off cooldown
        assertTrue(service.canClearAllCaches()); // Clear all still possible

        // Now clear all
        service.recordAllCachesCleared(Collections.singletonList(cacheName));
        assertFalse(service.canClearAllCaches()); // Clear all now on cooldown
        assertFalse(service.canClearCache(cacheName)); // Individual cache also on cooldown because of clear all
    }
}

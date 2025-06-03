package com.example.demo.controller;

import com.example.demo.service.CacheClearanceService;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List; // Ensure List is imported
import java.util.ArrayList; // Ensure ArrayList is imported
import com.example.demo.dto.CacheStatusDto; // Ensure DTO is imported


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MyBatisCacheControllerTest {

    @Mock(lenient = true)
    private SqlSessionFactory sqlSessionFactory;

    @Mock
    private CacheClearanceService cacheClearanceService;

    @Mock
    private Configuration mybatisConfiguration;

    @Mock
    private Cache cacheInstanceUser;

    @Mock
    private Cache cacheInstanceProduct;

    @InjectMocks
    private MyBatisCacheController controller;

    private final String USER_CACHE_NAME = "userCache";
    private final String PRODUCT_CACHE_NAME = "productCache";
    private final String UNKNOWN_CACHE_NAME = "unknownCache";

    @BeforeEach
    void setUp() {
        // Common mock setup
        when(sqlSessionFactory.getConfiguration()).thenReturn(mybatisConfiguration);
    }

    // Tests for clearSpecificCache
    @Test
    void clearSpecificCache_whenCooldownActive_shouldReturnTooManyRequests() {
        when(cacheClearanceService.canClearCache(USER_CACHE_NAME)).thenReturn(false);

        ResponseEntity<String> response = controller.clearSpecificCache(USER_CACHE_NAME);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertTrue(response.getBody().contains("Cooldown period active"));
        verify(cacheClearanceService).canClearCache(USER_CACHE_NAME);
        verifyNoInteractions(sqlSessionFactory);
    }

    @Test
    void clearSpecificCache_whenCacheNotFound_shouldReturnNotFound() {
        when(cacheClearanceService.canClearCache(UNKNOWN_CACHE_NAME)).thenReturn(true);
        when(mybatisConfiguration.hasCache(UNKNOWN_CACHE_NAME)).thenReturn(false);

        ResponseEntity<String> response = controller.clearSpecificCache(UNKNOWN_CACHE_NAME);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("Cache not found"));
        verify(cacheClearanceService).canClearCache(UNKNOWN_CACHE_NAME);
        verify(mybatisConfiguration).hasCache(UNKNOWN_CACHE_NAME);
    }

    @Test
    void clearSpecificCache_whenSuccessful_shouldReturnOkAndClearCache() {
        when(cacheClearanceService.canClearCache(USER_CACHE_NAME)).thenReturn(true);
        when(mybatisConfiguration.hasCache(USER_CACHE_NAME)).thenReturn(true);
        when(mybatisConfiguration.getCache(USER_CACHE_NAME)).thenReturn(cacheInstanceUser);

        ResponseEntity<String> response = controller.clearSpecificCache(USER_CACHE_NAME);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Cache cleared successfully"));
        verify(cacheClearanceService).canClearCache(USER_CACHE_NAME);
        verify(mybatisConfiguration).hasCache(USER_CACHE_NAME);
        verify(mybatisConfiguration).getCache(USER_CACHE_NAME);
        verify(cacheInstanceUser).clear();
        verify(cacheClearanceService).recordCacheCleared(USER_CACHE_NAME);
    }

    // Tests for clearAllCaches
    @Test
    void clearAllCaches_whenCooldownActive_shouldReturnTooManyRequests() {
        when(cacheClearanceService.canClearAllCaches()).thenReturn(false);

        ResponseEntity<String> response = controller.clearAllCaches();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertTrue(response.getBody().contains("Cooldown period active for clearing all caches"));
        verify(cacheClearanceService).canClearAllCaches();
        verifyNoInteractions(sqlSessionFactory);
    }

    @Test
    void clearAllCaches_whenNoCachesConfigured_shouldReturnOk() {
        when(cacheClearanceService.canClearAllCaches()).thenReturn(true);
        when(mybatisConfiguration.getCacheNames()).thenReturn(Collections.emptySet());

        ResponseEntity<String> response = controller.clearAllCaches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("No MyBatis caches configured"));
        verify(cacheClearanceService).canClearAllCaches();
        verify(mybatisConfiguration).getCacheNames();
        verify(cacheClearanceService).recordAllCachesCleared(Collections.emptySet());
    }

    @Test
    void clearAllCaches_whenSuccessful_shouldReturnOkAndClearAllConfiguredCaches() {
        when(cacheClearanceService.canClearAllCaches()).thenReturn(true);
        when(mybatisConfiguration.getCacheNames()).thenReturn(new HashSet<>(Arrays.asList(USER_CACHE_NAME, PRODUCT_CACHE_NAME)));
        when(mybatisConfiguration.getCache(USER_CACHE_NAME)).thenReturn(cacheInstanceUser);
        when(mybatisConfiguration.getCache(PRODUCT_CACHE_NAME)).thenReturn(cacheInstanceProduct);

        ResponseEntity<String> response = controller.clearAllCaches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("All MyBatis caches cleared successfully"));
        assertTrue(response.getBody().contains(USER_CACHE_NAME));
        assertTrue(response.getBody().contains(PRODUCT_CACHE_NAME));

        verify(cacheClearanceService).canClearAllCaches();
        verify(mybatisConfiguration).getCacheNames();
        verify(mybatisConfiguration).getCache(USER_CACHE_NAME);
        verify(cacheInstanceUser).clear();
        verify(mybatisConfiguration).getCache(PRODUCT_CACHE_NAME);
        verify(cacheInstanceProduct).clear();
        verify(cacheClearanceService).recordAllCachesCleared(new HashSet<>(Arrays.asList(USER_CACHE_NAME, PRODUCT_CACHE_NAME)));
    }

    @Test
    void clearAllCaches_whenOneCacheIsNull_shouldStillAttemptToClearOthers() {
        when(cacheClearanceService.canClearAllCaches()).thenReturn(true);
        when(mybatisConfiguration.getCacheNames()).thenReturn(new HashSet<>(Arrays.asList(USER_CACHE_NAME, "nullCache")));
        when(mybatisConfiguration.getCache(USER_CACHE_NAME)).thenReturn(cacheInstanceUser);
        when(mybatisConfiguration.getCache("nullCache")).thenReturn(null);

        ResponseEntity<String> response = controller.clearAllCaches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("All MyBatis caches cleared successfully"));

        verify(cacheInstanceUser).clear();
        verify(mybatisConfiguration).getCache("nullCache");
        verify(cacheClearanceService).recordAllCachesCleared(new HashSet<>(Arrays.asList(USER_CACHE_NAME, "nullCache")));
    }

    // --- Tests for /status/{cacheName} ---

    @Test
    void getCacheStatus_whenCacheNotFound_shouldReturnNotFound() {
        // when(cacheClearanceService.getCooldownMillis()).thenReturn(60000L); // Removed as it's not used when cache is not found
        when(sqlSessionFactory.getConfiguration().hasCache(UNKNOWN_CACHE_NAME)).thenReturn(false);

        ResponseEntity<?> response = controller.getCacheStatus(UNKNOWN_CACHE_NAME);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Cache not found"));
    }

    @Test
    void getCacheStatus_whenCooldownInactive_shouldReturnCorrectDto() {
        long now = System.currentTimeMillis();
        long cooldown = 60000L;
        when(sqlSessionFactory.getConfiguration().hasCache(USER_CACHE_NAME)).thenReturn(true);
        when(cacheClearanceService.getCooldownMillis()).thenReturn(cooldown);
        when(cacheClearanceService.getLastIndividualClearTimestamp(USER_CACHE_NAME)).thenReturn(now - cooldown * 2);
        when(cacheClearanceService.getLastClearAllTimestamp()).thenReturn(now - cooldown * 3);


        ResponseEntity<?> responseEntity = controller.getCacheStatus(USER_CACHE_NAME);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        com.example.demo.dto.CacheStatusDto dto = (com.example.demo.dto.CacheStatusDto) responseEntity.getBody();

        assertNotNull(dto);
        assertEquals(USER_CACHE_NAME, dto.getCacheName());
        assertFalse(dto.isCooldownActive());
        assertEquals(0, dto.getTimeUntilNextClearSeconds());
    }

    @Test
    void getCacheStatus_whenCooldownActive_shouldReturnCorrectDto() {
        long now = System.currentTimeMillis();
        long cooldown = 60000L; // 60 seconds
        long lastClearedJustNow = now - 10000; // Cleared 10 seconds ago

        when(sqlSessionFactory.getConfiguration().hasCache(USER_CACHE_NAME)).thenReturn(true);
        when(cacheClearanceService.getCooldownMillis()).thenReturn(cooldown);
        when(cacheClearanceService.getLastIndividualClearTimestamp(USER_CACHE_NAME)).thenReturn(lastClearedJustNow);
        when(cacheClearanceService.getLastClearAllTimestamp()).thenReturn(0L);

        ResponseEntity<?> responseEntity = controller.getCacheStatus(USER_CACHE_NAME);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        com.example.demo.dto.CacheStatusDto dto = (com.example.demo.dto.CacheStatusDto) responseEntity.getBody();

        assertNotNull(dto);
        assertEquals(USER_CACHE_NAME, dto.getCacheName());
        assertTrue(dto.isCooldownActive());
        assertTrue(dto.getTimeUntilNextClearSeconds() > 0);
        assertTrue(dto.getTimeUntilNextClearSeconds() < cooldown / 1000);
        long expectedSeconds = (lastClearedJustNow + cooldown - now) / 1000;
        assertTrue(Math.abs(expectedSeconds - dto.getTimeUntilNextClearSeconds()) <= 1, "Expected seconds " + expectedSeconds + " but got " + dto.getTimeUntilNextClearSeconds());
    }

    @Test
    void getCacheStatus_whenGlobalCooldownActive_shouldReflectGlobalCooldown() {
        long now = System.currentTimeMillis();
        long cooldown = 60000L; // 60 seconds
        long lastGlobalClearJustNow = now - 10000;
        long specificCacheLastClearedLongAgo = now - cooldown * 2;


        when(sqlSessionFactory.getConfiguration().hasCache(USER_CACHE_NAME)).thenReturn(true);
        when(cacheClearanceService.getCooldownMillis()).thenReturn(cooldown);
        when(cacheClearanceService.getLastIndividualClearTimestamp(USER_CACHE_NAME)).thenReturn(specificCacheLastClearedLongAgo);
        when(cacheClearanceService.getLastClearAllTimestamp()).thenReturn(lastGlobalClearJustNow);

        ResponseEntity<?> responseEntity = controller.getCacheStatus(USER_CACHE_NAME);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        com.example.demo.dto.CacheStatusDto dto = (com.example.demo.dto.CacheStatusDto) responseEntity.getBody();

        assertNotNull(dto);
        assertEquals(USER_CACHE_NAME, dto.getCacheName());
        assertTrue(dto.isCooldownActive());
        long expectedSecondsGlobal = (lastGlobalClearJustNow + cooldown - now) / 1000;
        assertTrue(Math.abs(expectedSecondsGlobal - dto.getTimeUntilNextClearSeconds()) <= 1, "Expected seconds " + expectedSecondsGlobal + " but got " + dto.getTimeUntilNextClearSeconds());
    }


    // --- Tests for /status (all caches) ---

    @Test
    void getAllCacheStatuses_whenNoCachesConfigured_shouldReturnEmptyList() {
        when(sqlSessionFactory.getConfiguration().getCacheNames()).thenReturn(Collections.emptySet());

        ResponseEntity<java.util.List<com.example.demo.dto.CacheStatusDto>> response = controller.getAllCacheStatuses();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getAllCacheStatuses_whenMultipleCachesExist_shouldReturnListOfDtos() {
        long now = System.currentTimeMillis();
        long cooldown = 60000L;

        long userCacheLastCleared = now - 10000;
        when(cacheClearanceService.getLastIndividualClearTimestamp(USER_CACHE_NAME)).thenReturn(userCacheLastCleared);

        long productCacheLastCleared = now - cooldown * 2;
        when(cacheClearanceService.getLastIndividualClearTimestamp(PRODUCT_CACHE_NAME)).thenReturn(productCacheLastCleared);

        long globalClearTimestamp = now - 30000;
        when(cacheClearanceService.getLastClearAllTimestamp()).thenReturn(globalClearTimestamp);


        when(cacheClearanceService.getCooldownMillis()).thenReturn(cooldown);
        when(sqlSessionFactory.getConfiguration().getCacheNames()).thenReturn(new HashSet<>(Arrays.asList(USER_CACHE_NAME, PRODUCT_CACHE_NAME)));

        ResponseEntity<java.util.List<com.example.demo.dto.CacheStatusDto>> response = controller.getAllCacheStatuses();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        java.util.List<com.example.demo.dto.CacheStatusDto> dtos = response.getBody();
        assertNotNull(dtos);
        assertEquals(2, dtos.size());

        com.example.demo.dto.CacheStatusDto userDto = dtos.stream().filter(d -> d.getCacheName().equals(USER_CACHE_NAME)).findFirst().orElse(null);
        assertNotNull(userDto);
        assertTrue(userDto.isCooldownActive());
        long expectedUserSeconds = (userCacheLastCleared + cooldown - now) / 1000;
        assertTrue(Math.abs(expectedUserSeconds - userDto.getTimeUntilNextClearSeconds()) <= 1, "Expected user seconds " + expectedUserSeconds + " but got " + userDto.getTimeUntilNextClearSeconds());


        com.example.demo.dto.CacheStatusDto productDto = dtos.stream().filter(d -> d.getCacheName().equals(PRODUCT_CACHE_NAME)).findFirst().orElse(null);
        assertNotNull(productDto);
        assertTrue(productDto.isCooldownActive());
        long expectedProductSeconds = (globalClearTimestamp + cooldown - now) / 1000;
        assertTrue(Math.abs(expectedProductSeconds - productDto.getTimeUntilNextClearSeconds()) <= 1, "Expected product seconds " + expectedProductSeconds + " but got " + productDto.getTimeUntilNextClearSeconds());
    }
}

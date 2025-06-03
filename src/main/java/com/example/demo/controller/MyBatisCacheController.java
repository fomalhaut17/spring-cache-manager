package com.example.demo.controller;

import com.example.demo.service.CacheClearanceService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.session.Configuration;
import java.util.Collection;
import org.springframework.web.bind.annotation.GetMapping; // Added
import java.util.List; // Added
import java.util.ArrayList; // Added
import com.example.demo.dto.CacheStatusDto; // Added

@RestController
@RequestMapping("/api/mybatis/cache")
public class MyBatisCacheController {

    private final SqlSessionFactory sqlSessionFactory;
    private final CacheClearanceService cacheClearanceService;

    @Autowired
    public MyBatisCacheController(SqlSessionFactory sqlSessionFactory, CacheClearanceService cacheClearanceService) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.cacheClearanceService = cacheClearanceService;
    }

    @org.springframework.web.bind.annotation.PostMapping("/clear/{cacheName}")
    public org.springframework.http.ResponseEntity<String> clearSpecificCache(@org.springframework.web.bind.annotation.PathVariable String cacheName) {
        if (!cacheClearanceService.canClearCache(cacheName)) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                .body("Cooldown period active for cache: " + cacheName + ". Please try again later.");
        }

        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        if (!configuration.hasCache(cacheName)) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body("Cache not found or not configured for namespace: " + cacheName);
        }

        org.apache.ibatis.cache.Cache cache = configuration.getCache(cacheName);

        if (cache != null) {
            cache.clear();
            cacheClearanceService.recordCacheCleared(cacheName);
            return org.springframework.http.ResponseEntity.ok("Cache cleared successfully: " + cacheName);
        } else {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Cache was reported as existing but could not be retrieved: " + cacheName);
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/clear-all")
    public org.springframework.http.ResponseEntity<String> clearAllCaches() {
        if (!cacheClearanceService.canClearAllCaches()) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                .body("Cooldown period active for clearing all caches. Please try again later.");
        }

        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        java.util.Collection<String> cacheNames = configuration.getCacheNames();

        cacheClearanceService.recordAllCachesCleared(cacheNames);

        if (cacheNames.isEmpty()) {
            return org.springframework.http.ResponseEntity.ok("No MyBatis caches configured to clear.");
        }

        for (String cacheName : cacheNames) {
            org.apache.ibatis.cache.Cache cache = configuration.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        return org.springframework.http.ResponseEntity.ok("All MyBatis caches cleared successfully. Affected caches: " + cacheNames.toString());
    }

    @org.springframework.web.bind.annotation.GetMapping("/status/{cacheName}")
    public org.springframework.http.ResponseEntity<?> getCacheStatus(@org.springframework.web.bind.annotation.PathVariable String cacheName) {
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        if (!configuration.hasCache(cacheName)) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body("Cache not found: " + cacheName);
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cacheClearanceService.getCooldownMillis();

        long lastClearedSpecific = cacheClearanceService.getLastIndividualClearTimestamp(cacheName);
        long lastClearedAll = cacheClearanceService.getLastClearAllTimestamp();

        long effectiveLastClearedTime = Math.max(lastClearedSpecific, lastClearedAll);

        long nextAvailableClearTime = effectiveLastClearedTime + cooldownMillis;

        boolean cooldownActive = now < nextAvailableClearTime;
        long timeUntilNextClearSeconds = cooldownActive ? (nextAvailableClearTime - now) / 1000 : 0;

        com.example.demo.dto.CacheStatusDto statusDto = new com.example.demo.dto.CacheStatusDto(
            cacheName,
            cooldownActive,
            nextAvailableClearTime,
            timeUntilNextClearSeconds
        );
        return org.springframework.http.ResponseEntity.ok(statusDto);
    }

    @org.springframework.web.bind.annotation.GetMapping("/status")
    public org.springframework.http.ResponseEntity<java.util.List<com.example.demo.dto.CacheStatusDto>> getAllCacheStatuses() {
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        java.util.Collection<String> cacheNames = configuration.getCacheNames();
        java.util.List<com.example.demo.dto.CacheStatusDto> statuses = new java.util.ArrayList<>();

        if (cacheNames.isEmpty()) {
            return org.springframework.http.ResponseEntity.ok(statuses); // Return empty list
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cacheClearanceService.getCooldownMillis();
        long lastClearedAllGlobal = cacheClearanceService.getLastClearAllTimestamp(); // Global clear all timestamp

        for (String cacheName : cacheNames) {
            long lastClearedSpecific = cacheClearanceService.getLastIndividualClearTimestamp(cacheName);
            long effectiveLastClearedTime = Math.max(lastClearedSpecific, lastClearedAllGlobal);

            long nextAvailableClearTime = effectiveLastClearedTime + cooldownMillis;
            boolean cooldownActive = now < nextAvailableClearTime;
            long timeUntilNextClearSeconds = cooldownActive ? (nextAvailableClearTime - now) / 1000 : 0;

            statuses.add(new com.example.demo.dto.CacheStatusDto(
                cacheName,
                cooldownActive,
                nextAvailableClearTime,
                timeUntilNextClearSeconds
            ));
        }
        return org.springframework.http.ResponseEntity.ok(statuses);
    }
}

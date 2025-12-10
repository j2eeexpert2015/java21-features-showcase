package org.example.controller;

import org.example.service.MemoryService;
import org.example.service.MemoryService.AllocationResult;
import org.example.service.MemoryService.SurvivorStats;
import org.springframework.web.bind.annotation.*;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple memory load generation controller
 * No metrics/monitoring dependencies
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Create natural generational workload
     *
     * POST /api/memory/load/{shortLivedMB}/{survivorsMB}
     *
     * Example: POST /api/memory/load/80/20
     * - 80MB short-lived objects (young generation - dies immediately)
     * - 20MB survivors (old generation - kept alive, rotates)
     */
    @PostMapping("/load/{shortLivedMB}/{survivorsMB}")
    public Map<String, Object> createLoad(
            @PathVariable int shortLivedMB,
            @PathVariable int survivorsMB) {

        AllocationResult result = memoryService.createNaturalWorkload(shortLivedMB, survivorsMB);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("totalObjects", result.totalObjects());
        response.put("youngObjects", result.youngObjects());
        response.put("survivorObjects", result.survivorObjects());
        response.put("totalMB", String.format("%.2f", result.totalMB()));
        response.put("durationMs", String.format("%.2f", result.durationMs()));

        return response;
    }

    /**
     * Get survivor statistics
     *
     * GET /api/memory/survivors
     */
    @GetMapping("/survivors")
    public Map<String, Object> getSurvivors() {
        SurvivorStats stats = memoryService.getSurvivorStats();

        Map<String, Object> response = new HashMap<>();
        response.put("count", stats.count());
        response.put("sizeInMB", stats.sizeInMB());

        return response;
    }

    /**
     * Clear all survivors
     *
     * POST /api/memory/clear
     */
    @PostMapping("/clear")
    public Map<String, Object> clearSurvivors() {
        memoryService.clearSurvivors();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "cleared");
        response.put("survivors", memoryService.getSurvivorStats());

        return response;
    }

    /**
     * Get JVM information
     *
     * GET /api/memory/info
     */
    @GetMapping("/info")
    public Map<String, Object> getJvmInfo() {
        Map<String, Object> info = new HashMap<>();

        // Basic JVM info
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("jvmName", System.getProperty("java.vm.name"));

        // Memory info
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memory.getHeapMemoryUsage();

        Map<String, Object> heapInfo = new HashMap<>();
        heapInfo.put("usedMB", heapUsage.getUsed() / (1024 * 1024));
        heapInfo.put("committedMB", heapUsage.getCommitted() / (1024 * 1024));
        heapInfo.put("maxMB", heapUsage.getMax() / (1024 * 1024));
        heapInfo.put("usagePercent", String.format("%.2f",
                (heapUsage.getUsed() * 100.0) / heapUsage.getMax()));
        info.put("heap", heapInfo);

        // GC info
        Map<String, Map<String, Object>> gcInfo = new HashMap<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gcData = new HashMap<>();
            gcData.put("collectionCount", gc.getCollectionCount());
            gcData.put("collectionTimeMs", gc.getCollectionTime());
            gcInfo.put(gc.getName(), gcData);
        }
        info.put("garbageCollectors", gcInfo);

        // Survivor stats
        info.put("survivors", memoryService.getSurvivorStats());

        return info;
    }

    /**
     * Get GC statistics
     *
     * GET /api/memory/gc-stats
     */
    @GetMapping("/gc-stats")
    public Map<String, Object> getGcStats() {
        Map<String, Object> stats = new HashMap<>();

        // GC details
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gcData = new HashMap<>();
            gcData.put("collectionCount", gc.getCollectionCount());
            gcData.put("collectionTimeMs", gc.getCollectionTime());
            gcData.put("name", gc.getName());

            // Calculate average if collections happened
            if (gc.getCollectionCount() > 0) {
                gcData.put("avgCollectionTimeMs",
                        gc.getCollectionTime() / (double) gc.getCollectionCount());
            } else {
                gcData.put("avgCollectionTimeMs", 0.0);
            }

            stats.put(gc.getName().replace(" ", "_"), gcData);
        }

        // Memory stats
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memory.getHeapMemoryUsage();

        Map<String, Object> memoryStats = new HashMap<>();
        memoryStats.put("heapUsedMB", heapUsage.getUsed() / (1024 * 1024));
        memoryStats.put("heapMaxMB", heapUsage.getMax() / (1024 * 1024));
        memoryStats.put("heapUsagePercent",
                String.format("%.2f", (heapUsage.getUsed() * 100.0) / heapUsage.getMax()));
        stats.put("memory", memoryStats);

        return stats;
    }

    /**
     * Health check
     *
     * GET /api/memory/health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
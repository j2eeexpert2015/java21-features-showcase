package org.example.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple memory load generation service
 * Creates realistic generational workload patterns
 */
@Service
public class MemoryService {

    private static final int OBJECT_SIZE_MB = 10;
    private static final int BYTES_PER_MB = 1024 * 1024;
    private static final int MAX_SURVIVORS = 20; // Keep ~200MB of survivors

    // Hold references for survivor objects
    private volatile List<byte[]> survivors = new ArrayList<>();
    private final Object lock = new Object();

    /**
     * Natural generational workload
     *
     * @param shortLivedMB Short-lived objects in MB (dies immediately)
     * @param survivorsMB Survivor objects in MB (kept alive for rotation)
     * @return Result with allocation details
     */
    public AllocationResult createNaturalWorkload(int shortLivedMB, int survivorsMB) {
        long startTime = System.nanoTime();
        long totalBytes = 0;
        int youngCount = 0;
        int survivorCount = 0;

        // 1. Create short-lived objects (young generation)
        List<byte[]> youngObjects = new ArrayList<>();
        int shortLivedChunks = shortLivedMB / OBJECT_SIZE_MB;

        for (int i = 0; i < shortLivedChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            fillWithData(chunk);
            youngObjects.add(chunk);
            totalBytes += chunk.length;
            youngCount++;
        }

        // 2. Create survivor objects (old generation)
        List<byte[]> newSurvivors = new ArrayList<>();
        int survivorChunks = survivorsMB / OBJECT_SIZE_MB;

        for (int i = 0; i < survivorChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            fillWithData(chunk);
            newSurvivors.add(chunk);
            totalBytes += chunk.length;
            survivorCount++;
        }

        // 3. Rotate survivors naturally
        rotateSurvivors(newSurvivors);

        long durationNanos = System.nanoTime() - startTime;

        // youngObjects become garbage here
        return new AllocationResult(
                youngCount + survivorCount,
                totalBytes,
                durationNanos,
                youngCount,
                survivorCount
        );
    }

    /**
     * Get current survivor statistics
     */
    public SurvivorStats getSurvivorStats() {
        synchronized (lock) {
            long survivorBytes = (long) survivors.size() * OBJECT_SIZE_MB * BYTES_PER_MB;
            return new SurvivorStats(
                    survivors.size(),
                    survivorBytes / BYTES_PER_MB
            );
        }
    }

    /**
     * Clear all survivors
     */
    public void clearSurvivors() {
        synchronized (lock) {
            survivors.clear();
        }
    }

    /**
     * Rotate survivors - keep only recent ones
     */
    private void rotateSurvivors(List<byte[]> newSurvivors) {
        synchronized (lock) {
            survivors.addAll(newSurvivors);

            // Keep only last N survivors
            if (survivors.size() > MAX_SURVIVORS) {
                survivors = new ArrayList<>(
                        survivors.subList(survivors.size() - MAX_SURVIVORS, survivors.size())
                );
            }
        }
    }

    /**
     * Fill array with random data to prevent JVM optimizations
     */
    private void fillWithData(byte[] array) {
        ThreadLocalRandom.current().nextBytes(array);
    }

    /**
     * Allocation result data
     */
    public record AllocationResult(
            int totalObjects,
            long totalBytes,
            long durationNanos,
            int youngObjects,
            int survivorObjects
    ) {
        public double durationMs() {
            return durationNanos / 1_000_000.0;
        }

        public double totalMB() {
            return totalBytes / (double) (1024 * 1024);
        }
    }

    /**
     * Survivor statistics data
     */
    public record SurvivorStats(
            int count,
            long sizeInMB
    ) {}
}
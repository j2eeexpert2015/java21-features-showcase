package org.example.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MemoryService {

    private static final int OBJECT_SIZE_MB  = 10;
    private static final int BYTES_PER_MB    = 1024 * 1024;
    private static final int MAX_SURVIVORS   = 20; // ceiling: 200MB of long-lived objects

    private volatile List<byte[]> survivors = new ArrayList<>();
    private final Object lock = new Object();

    /**
     * Creates a natural generational heap workload.
     *
     * shortLivedMB  → allocated as local variables, become garbage on method return
     * survivorsMB   → added to the survivors list, promoted to old gen, rotated out gradually
     */
    public AllocationResult createNaturalWorkload(int shortLivedMB, int survivorsMB) {
        long startTime = System.nanoTime();
        long totalBytes = 0;
        int youngCount = 0;
        int survivorCount = 0;

        // Short-lived — local list, all chunks become garbage when method returns
        List<byte[]> youngObjects = new ArrayList<>();
        int shortLivedChunks = shortLivedMB / OBJECT_SIZE_MB;

        for (int i = 0; i < shortLivedChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            fillWithData(chunk);
            youngObjects.add(chunk);
            totalBytes += chunk.length;
            youngCount++;
        }

        // Survivors — passed to rotateSurvivors, held by class-level reference
        List<byte[]> newSurvivors = new ArrayList<>();
        int survivorChunks = survivorsMB / OBJECT_SIZE_MB;

        for (int i = 0; i < survivorChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            fillWithData(chunk);
            newSurvivors.add(chunk);
            totalBytes += chunk.length;
            survivorCount++;
        }

        rotateSurvivors(newSurvivors);

        long durationNanos = System.nanoTime() - startTime;

        // youngObjects goes out of scope here — 160MB becomes garbage
        return new AllocationResult(
                youngCount + survivorCount,
                totalBytes,
                durationNanos,
                youngCount,
                survivorCount
        );
    }

    /**
     * Adds new survivors, trims the oldest once the list exceeds MAX_SURVIVORS.
     * Synchronized because multiple JMeter threads call this concurrently.
     */
    private void rotateSurvivors(List<byte[]> newSurvivors) {
        synchronized (lock) {
            survivors.addAll(newSurvivors);

            if (survivors.size() > MAX_SURVIVORS) {
                survivors = new ArrayList<>(
                        survivors.subList(survivors.size() - MAX_SURVIVORS, survivors.size())
                );
            }
        }
    }

    /**
     * Fills the array with random data — prevents JIT dead-code elimination
     * and escape analysis from removing the allocations entirely.
     */
    private void fillWithData(byte[] array) {
        ThreadLocalRandom.current().nextBytes(array);
    }

    public record AllocationResult(
            int totalObjects,
            long totalBytes,
            long durationNanos,
            int youngObjects,
            int survivorObjects
    ) {
        public double durationMs() { return durationNanos / 1_000_000.0; }
        public double totalMB()    { return totalBytes / (double) (1024 * 1024); }
    }
}
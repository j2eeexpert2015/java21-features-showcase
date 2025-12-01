package org.example.concepts.zgc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * COMPILE: javac -d target/classes AllocationStallDemo.java
 *
 * =================================================================
 * 1. RUN COMMANDS:
 * =================================================================
 *
 * G1GC (PROBLEM - shows allocation stalls):
 *   java -Xms256m -Xmx256m -XX:+UseG1GC -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *
 * Generational ZGC (SOLUTION - no allocation stalls):
 *   java -Xms256m -Xmx256m -XX:+UseZGC -XX:+ZGenerational -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *
 * WITH JFR RECORDING (for JMC analysis):
 *   G1GC: java -Xms256m -Xmx256m -XX:+UseG1GC -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=g1gc-stalls.jfr -Xlog:gc*,gc+heap=debug:file=g1gc-stalls.log -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *   ZGC:  java -Xms256m -Xmx256m -XX:+UseZGC -XX:+ZGenerational -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=zgc-smooth.jfr -Xlog:gc*,gc+heap=debug:file=zgc-smooth.log -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *
 *  G1GC: java -Xms256m -Xmx256m -XX:+UseG1GC -XX:StartFlightRecording=duration=120s,filename=g1gc-stalls.jfr -Xlog:gc*,gc+heap=debug:file=g1gc-stalls.log -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *  ZGC:  java -Xms256m -Xmx256m -XX:+UseZGC -XX:+ZGenerational -XX:StartFlightRecording=duration=120s,filename=zgc-smooth.jfr -Xlog:gc*,gc+heap=debug:file=zgc-smooth.log -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *
 * EXTREME (guaranteed stalls with G1GC):
 *   java -Xms192m -Xmx192m -XX:+UseG1GC -cp target/classes org.example.concepts.zgc.AllocationStallDemo
 *
 * =================================================================
 * 2. GC LOG ANALYSIS:
 * =================================================================
 *
 * Check for allocation stalls in G1GC:
 *   grep "Allocation Stall" g1gc-stalls.log
 *   Expected output: "Allocation Stall: 1.234s" etc.
 *
 * Compare pause times:
 *   grep "Pause" g1gc-stalls.log | head -5
 *   grep "Pause" zgc-smooth.log | head -5
 *
 * Count allocation stalls:
 *   grep -c "Allocation Stall" g1gc-stalls.log
 *   grep -c "Allocation Stall" zgc-smooth.log
 *
 * =================================================================
 * 3. JMC ANALYSIS STEPS:
 * =================================================================
 *
 * 1. Start JMC: jmc
 * 2. Open JFR files: File → Open → select g1gc-stalls.jfr and zgc-smooth.jfr
 * 3. For each file:
 *    a. Click "Garbage Collections" in left navigation
 *    b. Look at "Pause Phase" or "Pause Duration" chart
 *    c. G1GC: Shows large spikes (10-100ms)
 *    d. ZGC: Shows tiny spikes (<1ms)
 * 4. Compare side-by-side:
 *    - G1GC: Tall bars indicating stop-the-world pauses
 *    - ZGC: Flat line with barely visible micro-pauses
 * 5. Check "Memory" tab:
 *    - G1GC: Sawtooth pattern (stop-the-world collections)
 *    - ZGC: Smooth pattern (concurrent collections)
 *
 * =================================================================
 * EXPECTED RESULTS:
 * =================================================================
 *
 * G1GC: Console shows "🚨 ALLOCATION STALL" messages
 *       GC logs show "Allocation Stall: X.XXXs"
 *       JMC shows large pause spikes (10-100ms)
 *
 * ZGC:  No allocation stall messages
 *       GC logs show "Pause: 0.XXXms"
 *       JMC shows micro-pauses (<1ms)
 */
public class AllocationStallDemo {
    private static final int MAX_OBJECTS = 100000;
    private final ConcurrentHashMap<Long, MemoryChunk> liveObjects = new ConcurrentHashMap<>();
    private final AtomicLong objectId = new AtomicLong();
    private volatile boolean running = true;
    private final Stats stats = new Stats();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=".repeat(70));
        System.out.println("ALLOCATION STALL DEMO: G1GC vs Generational ZGC");
        System.out.println("=".repeat(70));

        new AllocationStallDemo().run();
    }

    private void run() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.submit(this::burstAllocator);
        executor.submit(this::memoryPressurizer);
        executor.submit(this::garbageCreator);
        executor.submit(this::statsReporter);

        Thread.sleep(120000);

        running = false;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        printResults();
    }

    private void burstAllocator() {
        Random rand = new Random();
        while (running) {
            try {
                long start = System.currentTimeMillis();
                int allocated = 0;

                for (int i = 0; i < 200 && liveObjects.size() < MAX_OBJECTS; i++) {
                    liveObjects.put(objectId.incrementAndGet(), new MemoryChunk(rand.nextInt(4096) + 1024));
                    allocated++;
                    stats.created.incrementAndGet();
                }

                long elapsed = System.currentTimeMillis() - start;
                if (elapsed > 100) {
                    System.out.printf("🚨 ALLOCATION STALL: %d objects in %dms%n", allocated, elapsed);
                    stats.stalls.incrementAndGet();
                }

                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void memoryPressurizer() {
        List<byte[]> pressure = new ArrayList<>();
        Random rand = new Random();

        while (running) {
            try {
                for (int i = 0; i < 30; i++) {
                    pressure.add(new byte[rand.nextInt(8192) + 2048]);
                }

                if (pressure.size() > 8000) {
                    pressure.subList(0, 4000).clear();
                }

                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void garbageCreator() {
        while (running) {
            try {
                if (liveObjects.size() > 40000) {
                    Iterator<Long> it = liveObjects.keySet().iterator();
                    int removed = 0;
                    while (it.hasNext() && removed < 500) {
                        it.next();
                        it.remove();
                        removed++;
                        stats.collected.incrementAndGet();
                    }
                }
                Thread.sleep(8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void statsReporter() {
        while (running) {
            try {
                double usage = (double) liveObjects.size() / MAX_OBJECTS * 100;
                System.out.printf("[%tT] Live: %6d | Created: %8d | Collected: %8d | Usage: %5.1f%%%n",
                        new Date(), liveObjects.size(), stats.created.get(),
                        stats.collected.get(), usage);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void printResults() {
        System.out.println("=".repeat(70));
        System.out.println("RESULTS:");
        System.out.printf("Objects created:   %,d%n", stats.created.get());
        System.out.printf("Objects collected: %,d%n", stats.collected.get());
        System.out.printf("Allocation stalls: %,d%n", stats.stalls.get());
        System.out.println("=".repeat(70));
    }

    static class MemoryChunk {
        final int size;
        final byte[] data;

        MemoryChunk(int size) {
            this.size = size;
            this.data = new byte[size];
        }
    }

    static class Stats {
        AtomicLong created = new AtomicLong();
        AtomicLong collected = new AtomicLong();
        AtomicLong stalls = new AtomicLong();
    }
}
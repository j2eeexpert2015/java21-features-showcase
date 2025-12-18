package org.example.concepts.zgc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GC Comparison Demo: Retail Workload Simulation
 *
 * PURPOSE:
 * Demonstrates performance differences between G1GC, Non-Generational ZGC, and Generational ZGC
 * using a realistic e-commerce workload with:
 * - LONG-LIVED: Configurable product catalog (Old Gen)
 * - SHORT-LIVED: Continuous Order creation (Young Gen)
 *
 * ============================================================================
 * RUN SCENARIOS
 * ============================================================================
 *
 * 1. COMPILE:
 *    mvn clean compile
 *
 * 2. RUN (choose one):
 *
 * RECOMMENDED: Use 512MB heap with BURST MODE settings to demonstrate allocation stalls!
 * With 400MB catalog + burst mode, 512MB heap creates tight memory conditions.
 *
 * PREPARATION:
 * mkdir -p logs jfr
 *
 * [SCENARIO A] G1GC (The Standard):
 * java -cp target/classes -Xmx512M -Xms512M -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Expected: Frequent Young GC, 10-50ms pauses, stable throughput with occasional dips
 *
 *
 * [SCENARIO B] Non-Generational ZGC (The Problem):
 * java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-nongen.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-nongen.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Expected: High heap pressure (90%+), ALLOCATION STALLS, throughput drops to ZERO
 * Verify: grep "Allocation Stall" logs/zgc-nongen.log | grep -v "0 / 0"  # Should show stalls!
 * JFR Analysis: Open jfr/zgc-nongen.jfr in JDK Mission Control, search for "ZAllocationStall" events
 *
 *
 * [SCENARIO C] Generational ZGC (The Solution):
 * java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/zgc-gen.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-gen.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Expected: Efficient heap usage, frequent Minor GC, <1ms pauses, ZERO stalls, rock-solid throughput
 * Verify: grep "Allocation Stalls:" logs/zgc-gen.log | grep -v "0        0        0        0"  # Should be empty
 * JFR Analysis: Open jfr/zgc-gen.jfr in JDK Mission Control, verify NO "ZAllocationStall" events
 *
 * ANALYZING JFR RECORDINGS:
 * 1. Open JDK Mission Control (jmc)
 * 2. File -> Open File -> Select jfr/*.jfr
 * 3. Navigate to "Event Browser"
 * 4. Search for:
 *    - "ZAllocationStall" (for ZGC stalls)
 *    - "GarbageCollection" (for GC events)
 *    - "ObjectAllocationInNewTLAB" (for allocation hotspots)
 * 5. Compare allocation stall counts across the three recordings
 *
 * OPTIONAL: Run with 1GB heap to see all GCs succeed (demonstrates heap sizing importance):
 *           Change -Xmx512M -Xms512M to -Xmx1G -Xms1G in all commands above
 *
 * ============================================================================
 * TUNING GUIDE: Configuration Constants
 * ============================================================================
 *
 * If you don't see allocation stalls with default settings, increase pressure:
 *
 * 1. Increase ORDERS_PER_ITERATION: 1 → 10 or 20
 *    Effect: 10-20x more allocation per thread iteration
 *
 * 2. Increase ITEMS_PER_ORDER: 50 → 100 or 200
 *    Effect: Larger ArrayList, more growth, more allocations
 *
 * 3. Increase ORDER_METADATA_SIZE: 1024 → 2048 or 4096
 *    Effect: Each order is heavier, more memory pressure
 *
 * 4. Set WAIT_FOR_PROFILER: true → false
 *    Effect: No manual pause, continuous allocation from start (CRITICAL for high allocation rate!)
 *
 * 5. Reduce heap: -Xmx512M → -Xmx384M or -Xmx256M
 *    Effect: Even less available space, guaranteed stalls
 *
 * Current settings are already aggressive (BURST MODE):
 * - ORDERS_PER_ITERATION = 20 (20x multiplier!)
 * - ITEMS_PER_ORDER = 100 (large orders)
 * - ORDER_METADATA_SIZE = 2048 (2KB each)
 * - WAIT_FOR_PROFILER = false (auto-start)
 *
 * These settings with 512MB heap should trigger allocation stalls in Non-Gen ZGC!
 */
public class RetailMemoryStress {

    // ========================================================================
    // WORKLOAD CONFIGURATION - Adjust these to control allocation pressure
    // ========================================================================

    /**
     * CATALOG SIZE: Controls long-lived data (Old Generation)
     * 200,000 = ~200MB | 400,000 = ~400MB | 600,000 = ~600MB
     */
    private static final int PRODUCT_CATALOG_COUNT = 400_000;

    /**
     * ALLOCATION INTENSITY: Controls how much garbage we create per iteration
     *
     * ORDERS_PER_ITERATION: How many orders to create per loop iteration
     * - Default: 1 (moderate pressure)
     * - Aggressive: 10-20 (high pressure, guaranteed stalls with 1GB heap)
     * - Nuclear: 50+ (extreme pressure)
     */
    private static final int ORDERS_PER_ITERATION = 1;

    /**
     * ITEMS_PER_ORDER: Number of products added to each order
     * - Default: 50 (realistic shopping cart)
     * - Aggressive: 100-200 (large orders)
     * - Effect: Forces ArrayList growth, more allocations
     */
    private static final int ITEMS_PER_ORDER = 50;

    /**
     * ORDER_METADATA_SIZE: Size of metadata byte array per order
     * - Default: 1024 bytes (1KB)
     * - Aggressive: 2048-4096 bytes (2-4KB)
     * - Effect: Each order is heavier
     */
    private static final int ORDER_METADATA_SIZE = 1024;

    /**
     * PROFILER ATTACHMENT: Wait for user input before starting workload
     * - true: Pause for VisualVM/JMC attachment (good for visual demos)
     * - false: Start immediately (recommended for triggering stalls)
     *
     * Note: Manual pause can reduce allocation rate due to JIT optimization!
     */
    private static final boolean WAIT_FOR_PROFILER = false;

    /**
     * AUTO START DELAY: Seconds to wait before starting (if not waiting for user)
     * Gives you time to attach profilers without killing allocation momentum
     */
    private static final int AUTO_START_DELAY_SECONDS = 5;

    // ========================================================================

    // LONG-LIVED DATA: Static catalog simulates production caches
    private static final List<Product> PRODUCT_CATALOG = new ArrayList<>();

    // Metrics to track application throughput impact
    private static final AtomicLong ordersProcessed = new AtomicLong(0);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        printHeader();
        printConfiguration();

        // STEP 1: Create long-lived data (Old Gen baseline)
        System.out.println("[Init] Loading Product Catalog (" + PRODUCT_CATALOG_COUNT + " objects)...");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < PRODUCT_CATALOG_COUNT; i++) {
            PRODUCT_CATALOG.add(new Product(i, "SKU-" + i, new byte[512]));
        }

        long loadTime = System.currentTimeMillis() - startTime;
        int catalogSizeMB = (PRODUCT_CATALOG_COUNT * 1024) / (1024 * 1024);
        System.out.println("[Init] ✓ Catalog loaded in " + loadTime + "ms (~" + catalogSizeMB + "MB)");
        System.out.println();

        // STEP 2: Pause or delay for profiler attachment
        if (WAIT_FOR_PROFILER) {
            waitForUserInput();
        } else {
            System.out.println("[Ready] Auto-starting in " + AUTO_START_DELAY_SECONDS + " seconds...");
            System.out.println("[Ready] Attach profilers now if needed (PID: " + ProcessHandle.current().pid() + ")");
            try {
                Thread.sleep(AUTO_START_DELAY_SECONDS * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[Ready] Starting workload NOW!");
            System.out.println();
        }

        // STEP 3: Start high-allocation workload
        System.out.println("[Action] Starting Black Friday Traffic Simulation...");
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("[Action] Worker threads: " + threads);
        System.out.println("[Action] Orders per iteration: " + ORDERS_PER_ITERATION);
        System.out.println("[Action] Items per order: " + ITEMS_PER_ORDER);

        int orderSize = ITEMS_PER_ORDER * 8 + ORDER_METADATA_SIZE + 100; // Rough estimate
        int allocationPerSec = threads * ORDERS_PER_ITERATION * orderSize * 1000; // Very rough
        System.out.println("[Action] Estimated allocation rate: ~" + (allocationPerSec / 1024 / 1024) + " MB/sec");
        System.out.println();

        for (int i = 0; i < threads; i++) {
            new Thread(RetailMemoryStress::generateTraffic, "Worker-" + i).start();
        }

        // STEP 4: Report throughput (watch for GC impact)
        System.out.println("[Reporter] Throughput reporting started...");
        System.out.println("[Reporter] Format: [Xs] Throughput: Y orders/sec | Total: Z");
        System.out.println("[Reporter] Watch for drops (GC pauses) or zeros (allocation stalls)");
        System.out.println();

        reportThroughput();
    }

    /**
     * Worker thread: Creates short-lived Orders continuously
     *
     * This is where the magic happens! Each iteration creates ORDERS_PER_ITERATION
     * orders, each with ITEMS_PER_ORDER items. All of these objects die immediately
     * after the iteration completes.
     *
     * GC Behavior:
     * - G1GC: Orders die in Eden, reclaimed by fast Young GC
     * - Non-Gen ZGC: No young/old separation → must scan catalog too → falls behind → STALLS
     * - Gen ZGC: Minor GC only scans young regions → ultra-fast → no stalls
     */
    private static void generateTraffic() {
        Random random = new Random();

        while (running) {
            // BURST MODE: Create multiple orders per iteration
            // Increase ORDERS_PER_ITERATION to amplify allocation pressure
            for (int burst = 0; burst < ORDERS_PER_ITERATION; burst++) {
                Order order = new Order(ORDER_METADATA_SIZE);

                // Add items to the order
                // Increase ITEMS_PER_ORDER for larger orders
                for (int i = 0; i < ITEMS_PER_ORDER; i++) {
                    Product p = PRODUCT_CATALOG.get(random.nextInt(PRODUCT_CATALOG.size()));
                    order.addItem(p);
                }

                ordersProcessed.incrementAndGet();

                // Order goes out of scope → garbage
                // Lifetime: microseconds to milliseconds
            }

            // No sleep, no delay - continuous allocation!
            // This is intentional to create maximum pressure
        }
    }

    /**
     * Reports throughput every second
     * GC pauses → throughput drops → visible in output
     */
    private static void reportThroughput() {
        long lastCount = 0;
        long startTime = System.currentTimeMillis();

        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            long currentCount = ordersProcessed.get();
            long throughput = currentCount - lastCount;
            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            System.out.printf("[%3ds] Throughput: %,7d orders/sec | Total: %,12d%n",
                    uptime, throughput, currentCount);

            lastCount = currentCount;
        }
    }

    private static void waitForUserInput() {
        long pid = ProcessHandle.current().pid();

        System.out.println("=================================================================");
        System.out.printf("   ⏸️  PAUSED | Process ID: %d%n", pid);
        System.out.println("   READY TO ATTACH PROFILER");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("📊 ATTACH YOUR TOOL:");
        System.out.println("   • VisualVM: Monitor tab → watch Heap graph");
        System.out.println("   • JMC: Start Flight Recording (60-120 sec)");
        System.out.println("   • GC logs already enabled (see -Xlog flag)");
        System.out.println();
        System.out.println("⏯️  Press [ENTER] to start the workload...");
        System.out.println();

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        scanner.nextLine();
        scanner.close();
    }

    private static void printHeader() {
        System.out.println("=================================================================");
        System.out.println("GC COMPARISON DEMO: Black Friday Sale Simulation");
        System.out.println("=================================================================");
        System.out.println();
    }

    private static void printConfiguration() {
        System.out.println("WORKLOAD CONFIGURATION:");
        System.out.println("  Catalog Size:         " + String.format("%,d", PRODUCT_CATALOG_COUNT) + " products (~" + (PRODUCT_CATALOG_COUNT / 1000) + "MB)");
        System.out.println("  Orders/Iteration:     " + ORDERS_PER_ITERATION + (ORDERS_PER_ITERATION > 1 ? " (BURST MODE!)" : ""));
        System.out.println("  Items/Order:          " + ITEMS_PER_ORDER);
        System.out.println("  Order Metadata:       " + ORDER_METADATA_SIZE + " bytes");
        System.out.println("  Wait for Profiler:    " + (WAIT_FOR_PROFILER ? "YES (manual start)" : "NO (auto-start in " + AUTO_START_DELAY_SECONDS + "s)"));
        System.out.println();

        if (ORDERS_PER_ITERATION > 5) {
            System.out.println("⚠️  BURST MODE ACTIVE! High allocation pressure expected.");
            System.out.println("   Non-Gen ZGC should show allocation stalls!");
            System.out.println();
        }
    }

    // DOMAIN OBJECTS

    /**
     * Product: Long-lived catalog item (~1KB each)
     * Lifetime: INFINITE (static list)
     * Promotes to Old Gen → ignored by frequent Minor GCs in Gen ZGC
     */
    record Product(int id, String sku, byte[] payload) {}

    /**
     * Order: Short-lived transaction
     * Lifetime: MILLISECONDS (created → populated → discarded)
     * Dies in Young Gen → perfect for Minor GC
     */
    static class Order {
        List<Product> items = new ArrayList<>();
        byte[] metaData;

        Order(int metadataSize) {
            this.metaData = new byte[metadataSize];
        }

        void addItem(Product p) {
            items.add(p);
        }
    }
}
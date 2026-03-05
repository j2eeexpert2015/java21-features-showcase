package org.example.concepts.zgc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
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
 * PREPARATION:
 * mkdir -p logs jfr
 * NOTE: logs/ and jfr/ directories are also created automatically by the application
 *
 * ============================================================================
 * PHASE 1: Run with 1GB heap (All GCs succeed - demonstrates capability)
 * ============================================================================
 *
 * G1GC with 1GB:
 * java -cp target/classes -Xmx1G -Xms1G -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Non-Gen ZGC with 1GB:
 * java -cp target/classes -Xmx1G -Xms1G -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Gen ZGC with 1GB:
 * java -cp target/classes -Xmx1G -Xms1G -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/generational-zgc-1g.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/generational-zgc-1g.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Expected with 1GB: All GCs handle the workload smoothly, rare or no stalls
 *
 * ============================================================================
 * PHASE 2: Run with 512MB heap (Shows GC differences - RECOMMENDED!)
 * ============================================================================
 *
 * G1GC with 512MB:
 * java -cp target/classes -Xmx512M -Xms512M -XX:+UseG1GC -Xlog:gc*:file=logs/g1gc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/g1gc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Non-Gen ZGC with 512MB:
 * java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:-ZGenerational -Xlog:gc*:file=logs/zgc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/zgc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * Gen ZGC with 512MB:
 * java -cp target/classes -Xmx512M -Xms512M -XX:+UseZGC -XX:+ZGenerational -Xlog:gc*:file=logs/generational-zgc-512m.log:time,level,tags -XX:StartFlightRecording=duration=60s,filename=jfr/generational-zgc-512m.jfr org.example.concepts.zgc.RetailMemoryStress
 *
 * ============================================================================
 * ANALYZING JFR RECORDINGS:
 * ============================================================================
 *
 * 1. Open JDK Mission Control (jmc)
 * 2. File -> Open File -> Select jfr/*.jfr
 * 3. Navigate to "Event Browser"
 * 4. Search for "ZAllocationStall" - compare counts across recordings
 * 5. Compare 1GB vs 512MB for same GC - see heap sizing impact
 * 6. Compare Non-Gen vs Gen ZGC at 512MB - see generational advantage
 *
 * ============================================================================
 * TUNING GUIDE: Configuration Constants
 * ============================================================================
 *
 * If you don't see allocation stalls with default settings, increase pressure:
 *
 * 1. Increase ORDERS_PER_ITERATION: 1 -> 10 or 20
 *    Effect: 10-20x more allocation per thread iteration
 *
 * 2. Increase ITEMS_PER_ORDER: 50 -> 100 or 200
 *    Effect: Larger ArrayList, more growth, more allocations
 *
 * 3. Increase ORDER_METADATA_SIZE: 1024 -> 2048 or 4096
 *    Effect: Each order is heavier, more memory pressure
 *
 * 4. Set WAIT_FOR_PROFILER: true -> false
 *    Effect: No manual pause, continuous allocation from start (CRITICAL for high allocation rate!)
 *
 * 5. Reduce heap: -Xmx512M -> -Xmx384M or -Xmx256M
 *    Effect: Even less available space, guaranteed stalls
 */
public class RetailMemoryStress {

    private static final int PRODUCT_CATALOG_COUNT    = 400_000;
    private static final int ORDERS_PER_ITERATION     = 1;
    private static final int ITEMS_PER_ORDER          = 50;
    private static final int ORDER_METADATA_SIZE      = 1024;
    private static final boolean WAIT_FOR_PROFILER    = false;
    private static final int AUTO_START_DELAY_SECONDS = 5;

    /*
     * Memory size constants (bytes)
     *
     * Product breakdown:
     *   Object header: 16 | int id: 4 | String ref: 4 | String obj: 24
     *   String byte[]: 26 | payload ref: 4 | byte[512]: 528 | padding: 4
     *   Total: 610 bytes per Product
     */
    private static final int PRODUCT_SIZE_BYTES = 610;

    /*
     * Order breakdown (live at peak):
     *   Order obj: 28 | ArrayList obj: 28 | final backing array (cap 73): 308
     *   byte[] metaData[1024]: 1,040
     *   Total live: 1,404 bytes
     */
    private static final int ORDER_LIVE_BYTES = 1_404;

    /*
     * ArrayList grows 10->15->22->33->49->73 while adding 50 items.
     * Each growth discards the old array — 5 discarded arrays = 596 bytes immediate garbage.
     */
    private static final int ORDER_DISCARDED_ARRAYS_BYTES = 596;

    /*
     * Total allocated per Order (live + discarded backing arrays) = 2,000 bytes
     */
    private static final int ORDER_TOTAL_ALLOCATED_BYTES =
            ORDER_LIVE_BYTES + ORDER_DISCARDED_ARRAYS_BYTES;

    private static final List<Product> PRODUCT_CATALOG = new ArrayList<>();
    private static final AtomicLong ordersProcessed = new AtomicLong(0);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        printHeader();
        printConfiguration();

        /* STEP 1: Create long-lived data (Old Gen baseline) */
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < PRODUCT_CATALOG_COUNT; i++) {
            PRODUCT_CATALOG.add(new Product(i, "SKU-" + i, new byte[512]));
        }
        long loadTime    = System.currentTimeMillis() - startTime;
        long catalogBytes = (long) PRODUCT_CATALOG_COUNT * PRODUCT_SIZE_BYTES
                + 16 + (long) PRODUCT_CATALOG_COUNT * 4; /* ArrayList backing array */

        System.out.printf("[Init] Catalog loaded in %dms -- Old Generation occupied: %,d objects x %d bytes = ~%.1f MB%n",
                loadTime, PRODUCT_CATALOG_COUNT, PRODUCT_SIZE_BYTES, catalogBytes / 1024.0 / 1024.0);
        System.out.println("[Init] Static reference -- always reachable, never collected. Permanent Old Gen pressure.");
        System.out.println();

        /* STEP 2: Pause or delay for profiler attachment */
        if (WAIT_FOR_PROFILER) {
            waitForUserInput();
        } else {
            System.out.println("[Ready] Auto-starting in " + AUTO_START_DELAY_SECONDS + " seconds... " +
                    "(PID: " + ProcessHandle.current().pid() + " -- attach JMC now if needed)");
            try {
                Thread.sleep(AUTO_START_DELAY_SECONDS * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[Ready] Starting workload NOW!");
            System.out.println();
        }

        /* STEP 3: Start high-allocation workload */
        int threads = Runtime.getRuntime().availableProcessors();

        System.out.println("[Action] Worker threads: " + threads);
        System.out.println("[Action] Per Order allocation breakdown:");
        System.out.println("[Action]   Order object:                  28 bytes");
        System.out.println("[Action]   ArrayList object:              28 bytes");
        System.out.println("[Action]   ArrayList backing (final):    308 bytes  (cap 73 after 50 items)");
        System.out.println("[Action]   byte[] metaData[1024]:      1,040 bytes");
        System.out.println("[Action]   -----------------------------------------");
        System.out.printf ("[Action]   Live size per Order:        %,d bytes  (~%.1f KB)%n",
                ORDER_LIVE_BYTES, ORDER_LIVE_BYTES / 1024.0);
        System.out.println("[Action]   ArrayList growth (10->15->22->33->49->73):");
        System.out.println("[Action]   Discarded backing arrays:      596 bytes  (immediate Young Gen garbage)");
        System.out.println("[Action]   -----------------------------------------");
        System.out.printf ("[Action]   Total allocated per Order:  %,d bytes  (~%.1f KB) -- all garbage within microseconds%n",
                ORDER_TOTAL_ALLOCATED_BYTES, ORDER_TOTAL_ALLOCATED_BYTES / 1024.0);
        System.out.println();

        for (int i = 0; i < threads; i++) {
            new Thread(RetailMemoryStress::generateTraffic, "Worker-" + i).start();
        }

        /* STEP 4: Report throughput — drops = GC pauses, zeros = allocation stalls */
        reportThroughput();
    }

    private static void generateTraffic() {
        Random random = new Random();
        while (running) {
            for (int burst = 0; burst < ORDERS_PER_ITERATION; burst++) {
                Order order = new Order(ORDER_METADATA_SIZE);

                /*
                 * ArrayList starts at capacity 10, grows to 73 after 50 addItem calls.
                 * Growth steps: 10 -> 15 -> 22 -> 33 -> 49 -> 73
                 * Each step allocates a new backing array and discards the old one.
                 */
                for (int i = 0; i < ITEMS_PER_ORDER; i++) {
                    Product p = PRODUCT_CATALOG.get(random.nextInt(PRODUCT_CATALOG.size()));
                    order.addItem(p);
                }
                ordersProcessed.incrementAndGet();
                /* order goes out of scope here — ~2KB back to Young Gen as garbage */
            }
            /* no sleep — continuous allocation */
        }
    }

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
            long throughput   = currentCount - lastCount;
            long uptime       = (System.currentTimeMillis() - startTime) / 1000;

            /* orders/sec x bytes per order = MB/sec into Young Gen */
            double allocMBps    = (throughput * ORDER_TOTAL_ALLOCATED_BYTES) / 1024.0 / 1024.0;
            double totalAllocMB = (currentCount * (double) ORDER_TOTAL_ALLOCATED_BYTES) / 1024.0 / 1024.0;

            System.out.printf("[%3ds] Orders/sec: %,7d | Allocated to Young Gen: ~%5.1f MB/sec | Total allocated to Young Gen: ~%,.0f MB%n",
                    uptime, throughput, allocMBps, totalAllocMB);

            lastCount = currentCount;
        }
    }

    private static void waitForUserInput() {
        System.out.println("=================================================================");
        System.out.printf ("   Process ID: %d%n", ProcessHandle.current().pid());
        System.out.println("   Attach JMC: Start Flight Recording (60-120 sec)");
        System.out.println("   GC logs already enabled via -Xlog flag");
        System.out.println("=================================================================");
        System.out.println("Press [ENTER] to start the workload...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
    }

    private static void printHeader() {
        System.out.println("=================================================================");
        System.out.println("GC COMPARISON DEMO: Retail Workload Simulation");
        System.out.println("=================================================================");
        System.out.println();
    }

    private static void printConfiguration() {
        Runtime rt = Runtime.getRuntime();
        System.out.println("SYSTEM INFO:");
        System.out.printf("  Java Version:    %s%n", System.getProperty("java.version"));
        System.out.printf("  OS:              %s (%s)%n", System.getProperty("os.name"), System.getProperty("os.arch"));
        System.out.printf("  CPU Cores:       %d%n", rt.availableProcessors());
        System.out.printf("  JVM Heap (-Xmx): %.0f MB%n", rt.maxMemory() / 1024.0 / 1024.0);
        System.out.println();

        System.out.println("WORKLOAD CONFIGURATION:");
        System.out.printf("  Catalog Size:      %,d products%n", PRODUCT_CATALOG_COUNT);
        System.out.printf("  Orders/Iteration:  %d%n", ORDERS_PER_ITERATION);
        System.out.printf("  Items/Order:       %d  (ArrayList growth: 10->15->22->33->49->73)%n", ITEMS_PER_ORDER);
        System.out.printf("  Order Metadata:    %,d bytes%n", ORDER_METADATA_SIZE);
        System.out.printf("  Wait for Profiler: %s%n",
                WAIT_FOR_PROFILER ? "YES (manual start)" : "NO (auto-start in " + AUTO_START_DELAY_SECONDS + "s)");
        System.out.println();

        long catalogMB = ((long) PRODUCT_CATALOG_COUNT * PRODUCT_SIZE_BYTES) / 1024 / 1024;
        System.out.println("MEMORY IMPACT:");
        System.out.printf("  Old Generation (permanent): ~%dMB  (%,d Products x %d bytes each)%n",
                catalogMB, PRODUCT_CATALOG_COUNT, PRODUCT_SIZE_BYTES);
        System.out.printf("  Per Order allocated:        ~%d bytes  (%d live + %d discarded ArrayList arrays)%n",
                ORDER_TOTAL_ALLOCATED_BYTES, ORDER_LIVE_BYTES, ORDER_DISCARDED_ARRAYS_BYTES);
        System.out.println();
    }

    /*
     * Product: Long-lived catalog item (~610 bytes each)
     * Lifetime: INFINITE (static list)
     * Promotes to Old Gen — ignored by Minor GCs in Gen ZGC
     */
    record Product(int id, String sku, byte[] payload) {}

    /*
     * Order: Short-lived transaction (~2KB allocated per instance)
     * Lifetime: MICROSECONDS (created -> populated -> discarded)
     * Dies in Young Gen — perfect for Minor GC
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
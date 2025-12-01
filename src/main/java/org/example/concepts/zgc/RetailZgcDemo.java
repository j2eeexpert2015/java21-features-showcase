package org.example.concepts.zgc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ============================================================
 * RetailZgcDemo
 * Demonstrating ZGC vs Generational ZGC using a Retail Workload
 * ============================================================
 *
 * This demo simulates a retail flash sale system that creates very high
 * object allocation pressure using millions of short-lived Order objects.
 *
 * The SAME Java program is executed multiple times using different garbage
 * collectors in order to show:
 *
 *   - Allocation pressure under G1GC
 *   - Allocation pressure under NON-generational ZGC
 *   - How Generational ZGC eliminates allocation stalls by introducing
 *     a young generation that efficiently absorbs short-lived objects
 *
 * The class provides built-in pause points so you can attach:
 *   - JDK Mission Control (JMC)
 *   - Java Flight Recorder (JFR)
 *
 * ------------------------------------------------------------
 * REQUIREMENTS
 * ------------------------------------------------------------
 * JDK 21+ required (Generational ZGC introduced in JDK 21).
 * JDK 23+: ZGC runs in generational mode by default.
 *
 * ------------------------------------------------------------
 * COMMON JVM OPTIONS
 * ------------------------------------------------------------
 *
 * -Xms4g                        Initial heap size = 4GB
 * -Xmx4g                        Maximum heap size = 4GB
 * -XX:+AlwaysPreTouch           Commit heap memory at startup (avoid page faults)
 * -XX:+UnlockExperimentalVMOptions Required to toggle ZGC generational mode (JDK 21)
 * -Xlog:gc*                     Enable GC logging (minor/major GC, pauses, heap usage)
 *
 * ------------------------------------------------------------
 * RUN COMMANDS (execute from project root)
 * ------------------------------------------------------------
 *
 * Assumes:
 *   Compiled classes -> target/classes
 *   Main class -> org.example.concepts.zgc.RetailZgcDemo
 *
 * ------------------------------------------------------------
 * 1) G1GC (Baseline)
 * ------------------------------------------------------------
 *
 * java -Xms4g -Xmx4g -XX:+UseG1GC -XX:+AlwaysPreTouch -XX:+UnlockExperimentalVMOptions -Xlog:gc* -cp target/classes org.example.concepts.zgc.RetailZgcDemo
 *
 * Shows:
 *   Traditional young/mixed collections with increased pause risk.
 *
 * ------------------------------------------------------------
 * 2) ZGC (NON-generational mode - JDK 21 only)
 * ------------------------------------------------------------
 *
 * java -Xms4g -Xmx4g -XX:+UseZGC -XX:-ZGenerational -XX:+AlwaysPreTouch -XX:+UnlockExperimentalVMOptions -Xlog:gc* -cp target/classes org.example.concepts.zgc.RetailZgcDemo
 *
 * Shows:
 *   Very low pauses but heavier GC CPU and possible allocation stalls
 *   under extreme allocation pressure.
 *
 * ------------------------------------------------------------
 * 3) GENERATIONAL ZGC (JDK 21+)
 * ------------------------------------------------------------
 *
 * java -Xms4g -Xmx4g -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+UnlockExperimentalVMOptions -Xlog:gc* -cp target/classes org.example.concepts.zgc.RetailZgcDemo
 *
 * Shows:
 *   Young generation absorbs short-lived objects
 *   Old generation remains mostly idle
 *   No allocation stalls
 *   Lower GC CPU cost
 *
 * ------------------------------------------------------------
 * NOTE FOR JDK 23+
 * ------------------------------------------------------------
 *
 * ZGC is generational by default.
 *
 * java -Xms4g -Xmx4g -XX:+UseZGC -Xlog:gc* -cp target/classes org.example.concepts.zgc.RetailZgcDemo
 *
 * ------------------------------------------------------------
 * HOW TO DEMO IN JMC
 * ------------------------------------------------------------
 *
 * Attach JMC when the app pauses, then start Flight Recording.
 * Focus on:
 *
 * Garbage Collections tab:
 *   - Pause Duration graph
 *   - Collection Type (Young vs Old in Gen ZGC)
 *   - Statistics (P99 / Max pause)
 *
 * Memory tab:
 *   - Allocation Rate
 *   - GC Threads (CPU usage)
 *
 * Events tab:
 *   - Allocation Stall / Allocation Failure events
 *
 * ------------------------------------------------------------
 * PURPOSE
 * ------------------------------------------------------------
 *
 * Same code. Same heap. Same workload.
 * Change ONE JVM option — the entire memory story changes.
 * ------------------------------------------------------------
 */
public class RetailZgcDemo {

    // ------------------- Load knobs -------------------
    private static final int WARMUP_SECONDS = 20;
    private static final int TEST_SECONDS   = 120;

    private static final int ORDERS_PER_BATCH   = 50_000;
    private static final int BATCHES_PER_SECOND = 20;

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        System.out.println("=== Retail ZGC / Gen ZGC Demo ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Start time: " + Instant.now());

        waitForEnter("JMC ATTACH POINT",
                "Attach JDK Mission Control now.\nStart Flight Recording before continuing.");

        warmupPhase();

        waitForEnter("BEFORE HIGH-ALLOCATION PHASE",
                "Warmup complete.\nRestart recording for clean graphs.\nPress ENTER to begin test phase.");

        highAllocationPhase();

        waitForEnter("END OF DEMO",
                "Workload finished.\nInspect GC behavior in JMC.\nPress ENTER to exit.");
    }

    private static void warmupPhase() {
        System.out.println(">>> WARMUP phase (" + WARMUP_SECONDS + "s)");
        long end = System.nanoTime() + WARMUP_SECONDS * 1_000_000_000L;
        while (System.nanoTime() < end) {
            processOrderBatch(false);
        }
        System.out.println(">>> WARMUP complete.");
    }

    private static void highAllocationPhase() {
        System.out.println(">>> HIGH-ALLOCATION TEST phase (" + TEST_SECONDS + "s)");
        long finish = System.nanoTime() + TEST_SECONDS * 1_000_000_000L;
        long targetBatchTime = 1_000_000_000L / BATCHES_PER_SECOND;
        long totalOrders = 0;

        while (System.nanoTime() < finish) {
            long start = System.nanoTime();

            totalOrders += processOrderBatch(true);

            long elapsed = System.nanoTime() - start;
            long remaining = targetBatchTime - elapsed;
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining / 1_000_000, (int) (remaining % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println(">>> Total orders created: " + String.format("%,d", totalOrders));
    }

    private static int processOrderBatch(boolean aggregate) {
        List<Order> batch = new ArrayList<>(ORDERS_PER_BATCH);
        for (int i = 0; i < ORDERS_PER_BATCH; i++) {
            batch.add(createOrder());
        }
        if (aggregate) {
            double total = 0;
            for (Order o : batch) {
                total += o.price;
            }
            if (total < 0) System.out.println("Impossible");
        }
        if (RANDOM.nextInt(10) == 0) retainSome(batch);
        return ORDERS_PER_BATCH;
    }

    private static Order createOrder() {
        return new Order(
                RANDOM.nextLong(1_000_000_000L),
                "cust-" + RANDOM.nextInt(100_000),
                "SKU-" + RANDOM.nextInt(10_000),
                1 + RANDOM.nextInt(3),
                RANDOM.nextDouble(10, 500),
                System.currentTimeMillis()
        );
    }

    private static final List<Order> RETAINED = new ArrayList<>();

    private static void retainSome(List<Order> batch) {
        for (int i = 0; i < batch.size(); i += 10) RETAINED.add(batch.get(i));
        if (RETAINED.size() > 1_000_000) RETAINED.clear();
    }

    private static void waitForEnter(String title, String msg) {
        System.out.println("\n=== " + title + " ===");
        System.out.println(msg);
        System.out.println("Press ENTER to continue...");
        try {
            while (System.in.read() != '\n') ;
        } catch (Exception ignored) {}
    }

    private static class Order {
        long id;
        String customer;
        String sku;
        int qty;
        double price;
        long created;

        Order(long id, String customer, String sku, int qty, double price, long created) {
            this.id = id;
            this.customer = customer;
            this.sku = sku;
            this.qty = qty;
            this.price = price;
            this.created = created;
        }
    }
}

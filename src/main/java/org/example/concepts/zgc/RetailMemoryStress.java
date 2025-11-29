package org.example.concepts.zgc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ZGC Demo: Retail Workload Simulation ("Black Friday")
 *
 * -------------------------------------------------------------------------
 * EXPERIMENT INSTRUCTIONS
 * -------------------------------------------------------------------------
 *
 * 1. RUN COMMANDS (Execute from project root after 'mvn clean compile')
 *
 * Scenario A: "The Old Way" (Non-Generational ZGC / Without Gen ZGC)
 * Command: java -cp target/classes -Xmx2G -XX:+UseZGC -XX:-ZGenerational org.example.concepts.zgc.RetailMemoryStress
 * Expectation: Throughput drops or stalls. High CPU scanning the 200MB Catalog repeatedly.
 *
 * Scenario B: "The New Way" (Generational ZGC / With Gen ZGC)
 * Command: java -cp target/classes -Xmx2G -XX:+UseZGC -XX:+ZGenerational org.example.concepts.zgc.RetailMemoryStress
 * Expectation: Stable throughput. Catalog is ignored. Young Gen is swept instantly.
 *
 * -------------------------------------------------------------------------
 * WHAT TO CHECK & WHEN
 * -------------------------------------------------------------------------
 *
 * 1. IN VISUALVM (Install "VisualGC" plugin first)
 * - When: Attach immediately when the app pauses at "READY TO ATTACH".
 * - Check: The "Visual GC" tab.
 * - Non-Gen Result: You see a single large space (often mapped to "Old Gen") fluctuating wildly.
 * There is no clear separation of short-lived vs long-lived data.
 * - Gen Result: You see distinct "Eden" vs "Old Gen" spaces.
 * "Eden" fills and clears instantly (Sawtooth pattern).
 * "Old Gen" (Catalog) stays completely flat and untouched.
 *
 * 2. IN JDK MISSION CONTROL (JMC)
 * - When: Start a "Flight Recording" (1 min) when the app pauses.
 * - Action: Go back to console and press [ENTER] to start the traffic.
 * - Check: "Event Browser" -> Search for "ZAllocationStall".
 * - Non-Gen Result: You will see "Allocation Stall" events (Application stopped!).
 * - Gen Result: You should see ZERO stalls (or significantly fewer).
 *
 * -------------------------------------------------------------------------
 */
public class RetailMemoryStress {

    // 1. Long-Lived Data: This list is static, so it is never garbage collected.
    // In Generational ZGC, these objects quickly move to the "Old Generation"
    // and are ignored by frequent young-gen collections.
    private static final List<Product> PRODUCT_CATALOG = new ArrayList<>();
    private static final int PRODUCT_CATALOG_COUNT = 200000;

    // Metrics to track simulation health
    private static final AtomicLong ordersProcessed = new AtomicLong(0);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        // --- Step 1: Populate Old Generation (The Setup) ---
        // Why we do this: Real servers aren't empty. They have caches.
        // We create ~200MB of "Live Data" to act as an obstacle for the GC.
        System.out.println("[Init] Loading Product Catalog (" + PRODUCT_CATALOG_COUNT + " objects)...");
        System.out.println("[Init] This simulates a 'warmed up' server with a full cache.");

        // Non-Gen ZGC must scan this 200MB repeatedly. Gen ZGC will ignore it.
        for (int i = 0; i < PRODUCT_CATALOG_COUNT; i++) {
            PRODUCT_CATALOG.add(new Product(i, "SKU-" + i, new byte[512]));
        }
        System.out.println("[Init] Catalog Loaded. Heap is now ~200MB full.");

        // --- Step 2: The JMC Hook (The Pause) ---
        // Why we wait here:
        // 1. It gives you time to attach JMC/VisualVM.
        // 2. It ensures the "Catalog Loading" noise doesn't show up in your recording.
        //    We only want to record the actual "Traffic" phase.
        waitForUserInput("READY TO ATTACH PROFILER (JMC/VisualVM)");

        // --- Step 3: Unleash the Load (Young Generation Churn) ---
        System.out.println("[Action] Starting Black Friday Traffic Simulation...");
        System.out.println("[Action] Spawning threads to create short-lived 'Order' objects...");

        int threads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < threads; i++) {
            new Thread(RetailMemoryStress::generateTraffic).start();
        }

        // --- Step 4: Monitor Throughput ---
        monitor();
    }

    private static void generateTraffic() {
        Random random = new Random();
        while (running) {
            // ALLOCATION: We create a new 'Order' object.
            // This is "Young Generation" data. It is used for milliseconds, then discarded.
            Order order = new Order();

            // PRESSURE: We add 50 items to the order.
            // Why: This forces the Allocation Rate (MB/sec) to spike efficiently.
            // We are trying to overwhelm the GC to force it into a decision.
            for (int i = 0; i < 50; i++) {
                Product p = PRODUCT_CATALOG.get(random.nextInt(PRODUCT_CATALOG.size()));
                order.addItem(p);
            }

            ordersProcessed.incrementAndGet();

            // GARBAGE: The 'order' variable goes out of scope here.
            // It is now garbage. Gen ZGC should clean this up instantly (Minor GC).
        }
    }

    private static void monitor() {
        long lastCount = 0;
        long startTime = System.currentTimeMillis();

        System.out.println("[Monitor] Tracking throughput. Watch for drops (Stalls)...");

        while (running) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            long currentCount = ordersProcessed.get();
            long throughput = currentCount - lastCount;
            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            // If throughput drops to near 0, we hit an Allocation Stall (JVM paused the app!)
            System.out.printf("[%ds] Throughput: %d orders/sec | Total: %d%n",
                    uptime, throughput, currentCount);

            lastCount = currentCount;
        }
    }

    private static void waitForUserInput(String stage) {
        long pid = ProcessHandle.current().pid();
        System.out.println("\n=================================================");
        System.out.printf("   APP PAUSED | PID: %d | %s%n", pid, stage);
        System.out.println("   --> Open JMC or VisualVM now.");
        System.out.println("   --> Connect to this PID.");
        System.out.println("   --> Start Recording.");
        System.out.println("   --> Then Press [ENTER] to start the chaos.");
        System.out.println("=================================================\n");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    // --- Domain Objects ---

    // Product: Long-lived object. Has a byte array payload to consume RAM.
    record Product(int id, String sku, byte[] payload) {}

    // Order: Short-lived object.
    static class Order {
        List<Product> items = new ArrayList<>();
        // Transaction Metadata: Extra garbage to increase allocation rate
        byte[] metaData = new byte[1024];

        void addItem(Product p) { items.add(p); }
    }
}
package org.example.concepts.zgc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class RetailMemoryStress {

    private static final int PRODUCT_CATALOG_COUNT = 600_000;
    private static final int ORDERS_PER_ITERATION = 20;
    private static final int ITEMS_PER_ORDER = 50;
    private static final int ORDER_METADATA_SIZE = 1024;
    private static final boolean WAIT_FOR_PROFILER = false;
    private static final int AUTO_START_DELAY_SECONDS = 5;
    private static final List<Product> PRODUCT_CATALOG = new ArrayList<>();
    private static final AtomicLong ordersProcessed = new AtomicLong(0);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        printHeader();
        printConfiguration();

        System.out.println("[Init] Loading Product Catalog (" + PRODUCT_CATALOG_COUNT + " objects)...");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < PRODUCT_CATALOG_COUNT; i++) {
            PRODUCT_CATALOG.add(new Product(i, "SKU-" + i, new byte[512]));
        }
        long loadTime = System.currentTimeMillis() - startTime;
        int catalogSizeMB = (PRODUCT_CATALOG_COUNT * 1024) / (1024 * 1024);
        System.out.println("[Init] Catalog loaded in " + loadTime + "ms (~" + catalogSizeMB + "MB)");
        System.out.println();

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

        System.out.println("[Action] Starting Black Friday Traffic Simulation...");
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("[Action] Worker threads: " + threads);
        System.out.println("[Action] Orders per iteration: " + ORDERS_PER_ITERATION);
        System.out.println("[Action] Items per order: " + ITEMS_PER_ORDER);
        int orderSize = ITEMS_PER_ORDER * 8 + ORDER_METADATA_SIZE + 100;
        int allocationPerSec = threads * ORDERS_PER_ITERATION * orderSize * 1000;
        System.out.println("[Action] Estimated allocation rate: ~" + (allocationPerSec / 1024 / 1024) + " MB/sec");
        System.out.println();

        for (int i = 0; i < threads; i++) {
            new Thread(RetailMemoryStress::generateTraffic, "Worker-" + i).start();
        }

        System.out.println("[Reporter] Throughput reporting started...");
        System.out.println("[Reporter] Format: [Xs] Throughput: Y orders/sec | Total: Z");
        System.out.println("[Reporter] Watch for drops (GC pauses) or zeros (allocation stalls)");
        System.out.println();

        reportThroughput();
    }

    private static void generateTraffic() {
        Random random = new Random();
        while (running) {
            for (int burst = 0; burst < ORDERS_PER_ITERATION; burst++) {
                Order order = new Order(ORDER_METADATA_SIZE);
                for (int i = 0; i < ITEMS_PER_ORDER; i++) {
                    Product p = PRODUCT_CATALOG.get(random.nextInt(PRODUCT_CATALOG.size()));
                    order.addItem(p);
                }
                ordersProcessed.incrementAndGet();
            }
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
        System.out.printf("   PAUSED | Process ID: %d%n", pid);
        System.out.println("   READY TO ATTACH PROFILER");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("ATTACH YOUR TOOL:");
        System.out.println("   VisualVM: Monitor tab -> watch Heap graph");
        System.out.println("   JMC: Start Flight Recording (60-120 sec)");
        System.out.println("   GC logs already enabled (see -Xlog flag)");
        System.out.println();
        System.out.println("Press [ENTER] to start the workload...");
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
            System.out.println("WARNING: BURST MODE ACTIVE! High allocation pressure expected.");
            System.out.println("   Non-Gen ZGC should show allocation stalls!");
            System.out.println();
        }
    }

    record Product(int id, String sku, byte[] payload) {}

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
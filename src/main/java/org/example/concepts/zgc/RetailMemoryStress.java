package org.example.concepts.zgc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class RetailMemoryStress {

    // 1. Long-Lived Data (Product Catalog) - Stays in Old Gen
    private static final List<Product> PRODUCT_CATALOG = new ArrayList<>();

    // 2. Metric Tracker
    private static final AtomicLong ordersProcessed = new AtomicLong(0);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        printHeader();

        // Step 1: Initialize Long-Lived Data
        System.out.println("[Init] Loading Product Catalog (200,000 objects)...");
        for (int i = 0; i < 200_000; i++) {
            PRODUCT_CATALOG.add(new Product(i, "SKU-" + i, new byte[512]));
        }
        System.out.println("[Init] Catalog Loaded. Memory populated.");

        // Step 2: Wait for User (Time to attach JMC/VisualVM)
        waitForUserInput("ready to attach JMC/VisualVM");

        // Step 3: Start High-Churn Threads
        System.out.println("[Action] Starting Black Friday Traffic Simulation...");
        int threads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < threads; i++) {
            new Thread(RetailMemoryStress::generateTraffic).start();
        }

        // Step 4: Monitor Output
        monitor();
    }

    private static void waitForUserInput(String stage) {
        System.out.println("\n=================================================");
        System.out.printf("App is paused. Process ID (PID): %d%n", ProcessHandle.current().pid());
        System.out.println("-> Press [ENTER] below to proceed.");
        System.out.println("=================================================\n");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private static void generateTraffic() {
        Random random = new Random();
        while (running) {
            // Mimic a user request: Create Order (Young Gen), add items, discard
            Order order = new Order();

            // Allocation Pressure: Create 50 objects per transaction
            for (int i = 0; i < 50; i++) {
                Product p = PRODUCT_CATALOG.get(random.nextInt(PRODUCT_CATALOG.size()));
                order.addItem(p);
            }

            // Update metric
            ordersProcessed.incrementAndGet();

            // 'order' falls out of scope here -> becomes Garbage immediately
        }
    }

    private static void monitor() {
        long lastCount = 0;
        long startTime = System.currentTimeMillis();

        while (running) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            long currentCount = ordersProcessed.get();
            long throughput = currentCount - lastCount;
            long uptime = (System.currentTimeMillis() - startTime) / 1000;

            System.out.printf("[%ds] Throughput: %d transactions/sec | Total: %d%n",
                    uptime, throughput, currentCount);

            lastCount = currentCount;
        }
    }

    private static void printHeader() {
        System.out.println("************************************************");
        System.out.println("   ZGC RETAIL DEMO - BLACK FRIDAY SIMULATION    ");
        System.out.println("************************************************");
    }

    // --- Domain Objects ---

    // Heavy object (simulating metadata) to occupy Heap space
    record Product(int id, String name, byte[] payload) {}

    static class Order {
        List<Product> items = new ArrayList<>();
        // Extra payload to fill memory faster (Allocation Pressure)
        byte[] transactionMetadata = new byte[1024];

        void addItem(Product p) { items.add(p); }
    }
}
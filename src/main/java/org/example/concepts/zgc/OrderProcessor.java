package org.example.concepts.zgc;

import java.util.*;
import java.util.concurrent.*;

/*
 * E-Commerce Order Processing - GC Demonstration
 *
 * This demo compares G1 GC vs Generational ZGC behavior under realistic workload.
 *
 * ==================================================================================
 * TEST CONFIGURATION:
 * ==================================================================================
 *
 * RUN WITH G1 GC:
 * java -XX:+UseG1GC -Xms512m -Xmx512m -cp target/classes org.example.concepts.zgc.OrderProcessor
 * java -XX:+UseG1GC -Xms256m -Xmx256m -cp target/classes org.example.concepts.zgc.OrderProcessor
 *
 * RUN WITH GENERATIONAL ZGC (Java 21-22):
 * java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -cp target/classes org.example.concepts.zgc.OrderProcessor
 *
 * RUN WITH GENERATIONAL ZGC (Java 23+):
 * java -XX:+UseZGC -Xms512m -Xmx512m -cp target/classes org.example.concepts.zgc.OrderProcessor
 *
 * ==================================================================================
 * WHY 512 MB HEAP?
 * ==================================================================================
 * - Forces frequent GC activity for clear demonstration
 * - Makes G1 GC pauses visible in VisualVM (sawtooth pattern)
 * - Realistic for typical microservice deployments
 * - Ensures fair comparison under same memory pressure
 *
 * ==================================================================================
 * WHAT TO MONITOR:
 * ==================================================================================
 *
 * IN VISUALVM (Monitor tab → Heap graph):
 * - G1 GC:  Sharp sawtooth pattern with vertical drops (pause events)
 * - ZGC:    Flat, stable pattern with no visible drops (concurrent collection)
 *
 * IN JMC (Java Mission Control):
 * - G1 GC:  Automated Analysis → Pause times of 20-50ms
 * - ZGC:    Automated Analysis → Pause times of <1ms
 *           Event Browser → Check ZAllocationStall events (should be ZERO)
 *
 * ==================================================================================
 * EXPECTED RESULTS:
 * ==================================================================================
 * Metric              | G1 GC                    | ZGC
 * --------------------|--------------------------|-------------------------
 * Max Pause Time      | 20-50 ms                 | <1 ms
 * VisualVM Pattern    | Sawtooth (sharp drops)   | Flat (no drops)
 * Application Impact  | Visible pauses           | No visible pauses
 *
 * ==================================================================================
 * DEMO WORKFLOW:
 * ==================================================================================
 * 1. Creates 10,000 products in inventory cache (long-lived objects → Old Gen)
 * 2. Processes orders for 2 minutes (creates short-lived objects → Young Gen)
 * 3. Each order creates temporary validation, calculation, payment objects
 * 4. Simulates realistic e-commerce memory allocation patterns
 *
 * Author: Ayan Dutta
 * Website: https://learningfromexperience.org
 */
public class OrderProcessor {

    // Simulates product inventory cache (long-lived objects)
    private static final Map<String, Product> inventoryCache = new ConcurrentHashMap<>();

    // Simulates recent orders cache
    private static final Queue<Order> recentOrders = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_ORDERS = 1000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("==============================================");
        System.out.println("E-Commerce Order Processing - GC Demo");
        System.out.println("==============================================");
        printGCInfo();

        // Wait for user to connect monitoring tools
        waitForUserInput("\n▶ Connect VisualVM or JMC now! Press ENTER when ready to start...");

        // Initialize inventory (long-lived data in Old Gen)
        initializeInventory();

        // Simulate order processing for 2 minutes
        System.out.println("\n▶ Processing orders...");
        processOrders(120); // 120 seconds = 2 minutes

        System.out.println("\n✓ Demo complete!");

        // Wait before exiting so user can observe final state
        waitForUserInput("\n▶ Check your monitoring tool for final results. Press ENTER to exit...");
    }

    /**
     * Waits for user to press ENTER
     */
    private static void waitForUserInput(String message) {
        System.out.println(message);
        System.out.println("PID: " + ProcessHandle.current().pid());
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Initializes product inventory - creates long-lived objects
     */
    private static void initializeInventory() {
        System.out.println("\n▶ Loading inventory cache (long-lived objects)...");

        for (int i = 1; i <= 10000; i++) {
            Product product = new Product(
                    "PROD-" + i,
                    "Product " + i,
                    99.99 + (i % 100),
                    1000 + (i % 500)
            );
            inventoryCache.put(product.id, product);
        }

        System.out.println("✓ Loaded " + inventoryCache.size() + " products");
    }

    /**
     * Simulates continuous order processing
     * Creates LOTS of short-lived objects (testing Young Gen)
     */
    private static void processOrders(int durationSeconds) {
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        int orderCount = 0;

        while (System.currentTimeMillis() < endTime) {
            // Process one order (creates many temporary objects)
            Order order = processOrder(orderCount++);

            // Keep recent orders (some survive to Old Gen)
            recentOrders.offer(order);
            if (recentOrders.size() > MAX_RECENT_ORDERS) {
                recentOrders.poll();
            }

            // Print progress every 1000 orders
            if (orderCount % 1000 == 0) {
                System.out.printf("Processed %,d orders | Heap: %d MB%n",
                        orderCount,
                        Runtime.getRuntime().totalMemory() / (1024 * 1024));
            }

            // Small delay for realistic timing
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("\n✓ Total orders processed: " + orderCount);
    }

    /**
     * Processes a single order - creates temporary objects for GC demo
     * Each order generates short-lived garbage (validation, calculations, etc.)
     */
    private static Order processOrder(int orderId) {
        // 1. Generate random items (temporary list)
        int itemCount = 1 + ThreadLocalRandom.current().nextInt(5);
        List<String> itemIds = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            itemIds.add("PROD-" + ThreadLocalRandom.current().nextInt(1, 10001));
        }

        // 2. Calculate total (creates temporary calculation objects)
        double total = 0.0;
        for (String productId : itemIds) {
            Product product = inventoryCache.get(productId);
            if (product != null) {
                total += product.price;
            }
        }

        // 3. Create temporary data for "processing"
        // This simulates validation, payment processing, etc. - all short-lived
        String[] tempData = new String[10];
        for (int i = 0; i < 10; i++) {
            tempData[i] = "Processing-" + orderId + "-" + i;
        }

        // 4. Return order (will be added to recentOrders cache)
        return new Order("ORD-" + orderId, total);
    }

    private static void printGCInfo() {
        var gcBeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
        System.out.println("\nActive Garbage Collectors:");
        for (var gc : gcBeans) {
            System.out.println("  • " + gc.getName());
        }

        Runtime rt = Runtime.getRuntime();
        System.out.println("\nHeap Configuration:");
        System.out.println("  • Max Heap: " + (rt.maxMemory() / (1024 * 1024)) + " MB");
    }

    // === Simple Data Classes ===

    static class Product {
        String id;
        String name;
        double price;
        int stock;

        Product(String id, String name, double price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }
    }

    static class Order {
        String orderId;
        double total;

        Order(String orderId, double total) {
            this.orderId = orderId;
            this.total = total;
        }
    }
}
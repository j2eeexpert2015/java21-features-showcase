package org.example.concepts.zgc;

/**
 * Minimal retail-style workload to compare:
 *   - ZGC without generations
 *   - Generational ZGC
 *
 * It creates:
 *   - A long-lived product catalog
 *   - Many short-lived "order" objects in a tight loop
 *
 * There are "press ENTER" pauses:
 *   - Before the workload starts (attach JMC / VisualVM)
 *   - After the workload ends (inspect data before exit)
 */
public class RetailZgcDemo {

    // --- Tuning constants ---
    private static final int DURATION_SECONDS = 300;      // how long to run
    private static final int CATALOG_SIZE     = 1_000;    // number of products
    private static final int MAX_ITEMS_PER_ORDER = 5;     // short-lived objects per iteration

    public static void main(String[] args) {
        System.out.println("Starting RetailZgcDemo...");
        System.out.printf("Duration: %d seconds, Catalog size: %d%n",
                DURATION_SECONDS, CATALOG_SIZE);

        // Give you time to attach JMC / VisualVM before allocations start
        waitForEnter("Attach JMC / VisualVM now if you want.\nPress ENTER to start the workload...");

        // Long-lived catalog (survives for the entire JVM lifetime)
        Product[] catalog = createCatalog(CATALOG_SIZE);

        // Short-lived allocations happen inside this loop
        long endTimeMillis = System.currentTimeMillis() + DURATION_SECONDS * 1_000L;

        long orders = 0;
        long lastLogTime = System.currentTimeMillis();
        long lastOrders = 0;

        java.util.Random random = new java.util.Random();

        while (System.currentTimeMillis() < endTimeMillis) {
            // Simulate one "order" with a few items
            int itemCount = 1 + random.nextInt(MAX_ITEMS_PER_ORDER);
            CartItem[] items = new CartItem[itemCount];

            for (int i = 0; i < itemCount; i++) {
                Product p = catalog[random.nextInt(catalog.length)];
                int qty = 1 + random.nextInt(3);
                items[i] = new CartItem(p, qty); // short-lived objects
            }

            // Build a small string payload to generate extra garbage
            String payload = buildPayload(items);

            // Prevent JIT from completely discarding payload
            if (payload.hashCode() == 42) {
                System.out.println("Unreachable branch");
            }

            orders++;

            // Log once per second: throughput + simple heap usage
            long now = System.currentTimeMillis();
            if (now - lastLogTime >= 1_000L) {
                long deltaOrders = orders - lastOrders;
                double opsPerSec = deltaOrders * 1000.0 / (now - lastLogTime);

                Runtime rt = Runtime.getRuntime();
                long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                long totalMb = rt.totalMemory() / (1024 * 1024);

                System.out.printf(
                        "[%tT] totalOrders=%d, ~%.0f orders/sec, heap=%dMB/%dMB%n",
                        now, orders, opsPerSec, usedMb, totalMb
                );

                lastLogTime = now;
                lastOrders = orders;
            }
        }

        System.out.printf("Workload finished. Total orders processed: %,d%n", orders);

        // Pause so you can inspect JMC / VisualVM before JVM exits
        waitForEnter("Workload complete. Inspect JMC / VisualVM now.\nPress ENTER to exit...");
    }

    // --- Helper: simple stdin pause ---

    private static void waitForEnter(String message) {
        System.out.println();
        System.out.println(message);
        try {
            // Read a full line from stdin; ignore content
            new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
        } catch (java.io.IOException e) {
            // If stdin isn't available, just continue
            System.out.println("Could not read from stdin, continuing...");
        }
    }

    // --- Long-lived data ---

    private static Product[] createCatalog(int size) {
        Product[] catalog = new Product[size];
        for (int i = 0; i < size; i++) {
            double price = 5.0 + (i % 200); // just some varying price
            catalog[i] = new Product(i, "Product-" + i, price);
        }
        return catalog;
    }

    // --- Short-lived data per "order" ---

    private static String buildPayload(CartItem[] items) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        for (int i = 0; i < items.length; i++) {
            CartItem item = items[i];
            sb.append("{\"id\":").append(item.product.id)
                    .append(",\"name\":\"").append(item.product.name).append('"')
                    .append(",\"qty\":").append(item.quantity)
                    .append(",\"price\":").append(item.product.price)
                    .append('}');
            if (i < items.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    // --- Tiny domain classes (just enough to be "retail") ---

    static class Product {
        final int id;
        final String name;
        final double price;

        Product(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    static class CartItem {
        final Product product;
        final int quantity;

        CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }
}

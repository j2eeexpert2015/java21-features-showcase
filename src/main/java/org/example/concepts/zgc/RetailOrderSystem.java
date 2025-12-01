package org.example.concepts.zgc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * DEMO: Generational ZGC vs G1GC - Allocation Stall Comparison
 *
 * COMPILE:
 *   javac -d target/classes RetailOrderSystem.java
 *
 * QUICK DEMO (see basic behavior):
 *   java -Xms512m -Xmx512m -XX:+UseG1GC -cp target/classes org.example.concepts.zgc.RetailOrderSystem
 *   java -Xms512m -Xmx512m -XX:+UseZGC -XX:+ZGenerational -cp target/classes org.example.concepts.zgc.RetailOrderSystem
 *
 *
 * ALLOCATION STALL DEMO (see the real problem):
 *   java -Xms384m -Xmx384m -XX:+UseG1GC -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=g1gc-struggling.jfr -Xlog:gc*,gc+heap=debug:file=g1gc-detailed.log -cp target/classes org.example.concepts.zgc.RetailOrderSystem
 *   java -Xms384m -Xmx384m -XX:+UseZGC -XX:+ZGenerational -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=zgc-smooth.jfr -Xlog:gc*,gc+heap=debug:file=zgc-detailed.log -cp target/classes org.example.concepts.zgc.RetailOrderSystem
 *
 * ALLOCATION STALL DEMO (see the real problem):
 *   java -Xms256m -Xmx256m -XX:+UseG1GC -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=g1gc-struggling.jfr -Xlog:gc*,gc+heap=debug:file=g1gc-detailed.log -cp target/classes org.example.concepts.zgc.RetailOrderSystem
 *   java -Xms256m -Xmx256m -XX:+UseZGC -XX:+ZGenerational -XX:+FlightRecorder -XX:StartFlightRecording=duration=120s,filename=zgc-smooth.jfr -Xlog:gc*,gc+heap=debug:file=zgc-detailed.log -cp target/classes org.example.concepts.zgc.RetailOrderSystem
 *
 * GC LOG ANALYSIS:
 *   grep -E "(Allocation Stall|Pause|Full GC)" g1gc-detailed.log
 *   grep -E "(Pause|Allocation)" zgc-detailed.log
 *
 * JMC ANALYSIS STEPS:
 *   1. Start JMC: jmc
 *   2. Open JFR files: File → Open → select .jfr files
 *   3. Compare these views side-by-side:
 *      - Memory Tab: Object allocation rate, GC activity patterns
 *      - GC Tab: Pause times and GC cycle frequency
 *      - Code Tab: Methods causing most allocations
 *      - Latency Tab: Application stall events
 *      - Allocations Tab: TLAB success/failure rates
 *   4. Key differences to observe:
 *      G1GC: Spiky allocation rate, long pauses (50-200ms), allocation stalls, TLAB failures
 *      Generational ZGC: Smooth allocation, sub-millisecond pauses, no stalls, efficient TLAB usage
 *
 * EXPECTED RESULTS:
 *   G1GC: Allocation stalls (1-2 seconds), high pause times, application freezing
 *   Generational ZGC: No allocation stalls, consistent low latency, smooth operation
 */
public class RetailOrderSystem {
    private static final int YOUNG_ORDER_LIFETIME_MS = 100;    // Short-lived orders
    private static final int OLD_ORDER_LIFETIME_MS = 30000;    // Long-lived orders
    private static final int MAX_ACTIVE_ORDERS = 50000;

    private final ConcurrentHashMap<Long, RetailOrder> activeOrders = new ConcurrentHashMap<>();
    private final List<RetailOrder> completedOrders = new CopyOnWriteArrayList<>();
    private final AtomicLong orderIdGenerator = new AtomicLong();
    private volatile boolean running = true;
    private final Statistics stats = new Statistics();

    public static void main(String[] args) throws InterruptedException {
        RetailOrderSystem system = new RetailOrderSystem();
        system.start();
    }

    public void start() throws InterruptedException {
        System.out.println("🚀 Starting Retail Order System Demo");
        System.out.println("JVM: " + System.getProperty("java.vm.name"));
        System.out.println("GC: " + System.getProperty("java.vm.options"));
        System.out.println("Max Memory: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");
        System.out.println("==========================================");

        ExecutorService executor = Executors.newFixedThreadPool(6);

        // Start different workload generators
        executor.submit(() -> burstyOrderProducer(1000, 50));  // Bursty allocation
        executor.submit(() -> steadyOrderProducer(200));       // Steady allocation
        executor.submit(() -> memoryStressProducer());         // Creates memory pressure
        executor.submit(() -> orderProcessor());               // Processes orders
        executor.submit(() -> metricsReporter());              // Reports stats

        // Run demo for 2 minutes
        Thread.sleep(120000);

        running = false;
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("\n==========================================");
        System.out.println("Demo completed. Final statistics:");
        stats.printStats();
    }

    private void burstyOrderProducer(int burstSize, int delayBetweenBursts) {
        Random random = new Random();
        while (running) {
            try {
                // Create burst of orders
                for (int i = 0; i < burstSize; i++) {
                    createOrder(random, true); // Short-lived orders
                    stats.ordersCreated.incrementAndGet();
                }

                // Add some long-lived orders occasionally
                if (random.nextInt(10) == 0) {
                    for (int i = 0; i < 100; i++) {
                        createOrder(random, false); // Long-lived orders
                        stats.longLivedOrdersCreated.incrementAndGet();
                    }
                }

                Thread.sleep(delayBetweenBursts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void steadyOrderProducer(int ordersPerSecond) {
        Random random = new Random();
        long delayBetweenOrders = 1000 / ordersPerSecond;

        while (running) {
            try {
                createOrder(random, true);
                stats.ordersCreated.incrementAndGet();
                Thread.sleep(delayBetweenOrders);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void createOrder(Random random, boolean shortLived) {
        if (activeOrders.size() >= MAX_ACTIVE_ORDERS) {
            return; // Back pressure
        }

        long orderId = orderIdGenerator.incrementAndGet();
        RetailOrder order = new RetailOrder(orderId);

        // Add variable number of items (creates object churn)
        int itemCount = random.nextInt(15) + 1;
        for (int i = 0; i < itemCount; i++) {
            order.addItem(createOrderItem(random));
        }

        // Add customer data (creates string/array churn)
        order.setCustomer(createCustomer(random, orderId));

        // Set lifetime based on type
        order.setExpiryTime(System.currentTimeMillis() +
                (shortLived ? YOUNG_ORDER_LIFETIME_MS : OLD_ORDER_LIFETIME_MS));

        activeOrders.put(orderId, order);
        stats.lastOrderId = orderId;
    }

    private OrderItem createOrderItem(Random random) {
        // Create various sized items to create allocation pressure
        String[] products = {"Laptop", "Phone", "Tablet", "Headphones", "Monitor",
                "Keyboard", "Mouse", "Controller", "Charger", "Cable"};
        String product = products[random.nextInt(products.length)];

        // Create some garbage with temporary strings
        String sku = product.toUpperCase() + "-" + random.nextInt(10000) + "-" +
                System.currentTimeMillis() + "-" + Thread.currentThread().getId();

        return new OrderItem(
                sku.substring(0, Math.min(20, sku.length())), // Trim but create garbage
                random.nextDouble() * 1000,
                random.nextInt(5) + 1
        );
    }

    private Customer createCustomer(Random random, long orderId) {
        // Create customer with various data sizes (memory pressure)
        String[] names = {"John", "Jane", "Bob", "Alice", "Charlie", "Diana"};
        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com"};

        String name = names[random.nextInt(names.length)] + " " +
                names[random.nextInt(names.length)] + "son";

        String email = name.toLowerCase().replace(" ", ".") + orderId +
                "@" + domains[random.nextInt(domains.length)];

        // Create address with some garbage
        String street = "Street " + random.nextInt(1000) + " " +
                "Avenue".substring(0, random.nextInt(6) + 1);

        return new Customer(name, email,
                new Address(street, "City" + random.nextInt(100), "Zip" + random.nextInt(99999))
        );
    }

    private void memoryStressProducer() {
        List<byte[]> memoryPressure = new ArrayList<>();
        Random random = new Random();

        while (running) {
            try {
                // Create mixed allocation pattern
                for (int i = 0; i < 100; i++) {
                    int size = random.nextInt(8192) + 64; // 64B to 8KB
                    byte[] data = new byte[size];
                    memoryPressure.add(data);

                    // Occasionally create larger allocations
                    if (random.nextInt(100) == 0) {
                        byte[] largeData = new byte[random.nextInt(65536) + 16384]; // 16KB to 80KB
                        memoryPressure.add(largeData);
                    }
                }

                // Remove some to create garbage
                if (memoryPressure.size() > 50000) {
                    memoryPressure.subList(0, 25000).clear();
                    stats.memoryCollections.incrementAndGet();
                }

                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void orderProcessor() {
        Random random = new Random();

        while (running) {
            try {
                // Process expired orders (creates garbage when collected)
                long now = System.currentTimeMillis();
                Iterator<RetailOrder> it = activeOrders.values().iterator();
                int processed = 0;

                while (it.hasNext() && processed < 100) {
                    RetailOrder order = it.next();
                    if (order.getExpiryTime() <= now) {
                        it.remove();
                        order.process();
                        completedOrders.add(order);
                        stats.ordersProcessed.incrementAndGet();
                        processed++;
                    }
                }

                // Limit completed orders history
                if (completedOrders.size() > 20000) {
                    completedOrders.subList(0, 10000).clear();
                }

                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void metricsReporter() {
        while (running) {
            try {
                System.out.printf("[Stats] Active: %6d | Created: %8d | Processed: %8d | Long-lived: %6d%n",
                        activeOrders.size(),
                        stats.ordersCreated.get(),
                        stats.ordersProcessed.get(),
                        stats.longLivedOrdersCreated.get()
                );

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Data classes designed to create allocation pressure and object churn
     * to demonstrate GC behavior under high allocation rates
     */
    static class RetailOrder {
        private final long id;
        private final List<OrderItem> items = new ArrayList<>();
        private Customer customer;
        private double total;
        private long createTime;
        private long expiryTime;

        public RetailOrder(long id) {
            this.id = id;
            this.createTime = System.currentTimeMillis();
        }

        public void addItem(OrderItem item) {
            items.add(item);
            total += item.getPrice() * item.getQuantity();
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }

        public void setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public void process() {
            // Simulate processing - creates temporary objects
            String receipt = "Receipt for order " + id + " total: " + total;
            byte[] receiptData = receipt.getBytes();
            // receiptData becomes garbage immediately
        }
    }

    static class OrderItem {
        private final String sku;
        private final double price;
        private final int quantity;

        public OrderItem(String sku, double price, int quantity) {
            this.sku = sku;
            this.price = price;
            this.quantity = quantity;
        }

        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
    }

    static class Customer {
        private final String name;
        private final String email;
        private final Address address;

        public Customer(String name, String email, Address address) {
            this.name = name;
            this.email = email;
            this.address = address;
        }
    }

    static class Address {
        private final String street;
        private final String city;
        private final String zipCode;

        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
    }

    /**
     * Statistics tracking for demo analysis
     * Shows allocation rates, processing throughput, and memory behavior
     */
    static class Statistics {
        AtomicLong ordersCreated = new AtomicLong();
        AtomicLong ordersProcessed = new AtomicLong();
        AtomicLong longLivedOrdersCreated = new AtomicLong();
        AtomicLong memoryCollections = new AtomicLong();
        volatile long lastOrderId = 0;

        void printStats() {
            System.out.printf("Total orders created:    %,d%n", ordersCreated.get());
            System.out.printf("Total orders processed:  %,d%n", ordersProcessed.get());
            System.out.printf("Long-lived orders:       %,d%n", longLivedOrdersCreated.get());
            System.out.printf("Memory cycles:           %,d%n", memoryCollections.get());
        }
    }
}
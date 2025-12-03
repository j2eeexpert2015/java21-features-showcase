package org.example.concepts.zgc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Flash Sale GC Demo - Allocation Stall Comparison
 *
 * Demonstrates allocation stall behavior differences between
 * G1GC, Classic ZGC, and Generational ZGC under heavy retail workload.
 *
 * SETUP:
 *   mkdir -p target/classes logs jfr
 *   javac -d target/classes src/main/java/org/example/concepts/zgc/FlashSaleGCDemo.java
 *
 * RUN SCENARIOS (run mkdir command above first!):
 *
 * 1. G1GC (baseline - expect STW pauses)
 *    java -XX:+UseG1GC -Xmx256m -Xms256m -Xlog:gc*,gc+heap=debug:file=logs/g1gc.log:time,uptime -XX:StartFlightRecording=duration=120s,settings=profile,filename=jfr/g1gc.jfr -cp target/classes org.example.concepts.zgc.FlashSaleGCDemo
 *
 * 2. Classic ZGC (expect allocation stalls under pressure)
 *    java -XX:+UseZGC -XX:-ZGenerational -Xmx256m -Xms256m -Xlog:gc*,gc+heap=debug:file=logs/zgc-classic.log:time,uptime -XX:StartFlightRecording=duration=120s,settings=profile,filename=jfr/zgc-classic.jfr -cp target/classes org.example.concepts.zgc.FlashSaleGCDemo
 *
 * 3. Generational ZGC (zero allocation stalls - recommended)
 *    java -XX:+UseZGC -XX:+ZGenerational -Xmx256m -Xms256m -Xlog:gc*,gc+heap=debug:file=logs/zgc-gen.log:time,uptime -XX:StartFlightRecording=duration=120s,settings=profile,filename=jfr/zgc-gen.jfr -cp target/classes org.example.concepts.zgc.FlashSaleGCDemo
 *
 * JMC ANALYSIS (compare the three .jfr files in jfr/ directory):
 *   - Event Browser -> filter "ZAllocationStall" -> Count should be 0 only with Gen ZGC
 *   - Event Browser -> filter "GCPhasePause" -> Compare pause durations
 *   - JVM Internals -> GC -> Compare collection frequencies and pause times
 *   - Java Application -> Allocations -> Compare allocation rate stability
 *
 * EXPECTED RESULTS:
 *   | Metric              | G1GC        | Classic ZGC    | Gen ZGC      |
 *   |---------------------|-------------|----------------|--------------|
 *   | Allocation Stalls   | N/A         | Multiple       | Zero/Minimal |
 *   | Max Pause Time      | 50-200ms    | <1ms (pauses)  | <1ms         |
 *   | Latency Spikes      | Periodic    | During bursts  | Rare         |
 *   | Throughput          | Good        | Good           | Best         |
 */
public class FlashSaleGCDemo {

    private static final int BASE_ORDERS_PER_SEC = 5000;
    private static final int BURST_ORDERS_PER_SEC = 25000;
    private static final int BURST_DURATION_SEC = 15;
    private static final int BURST_INTERVAL_SEC = 18;
    private static final int RUN_DURATION_SEC = 90;
    private static final int CACHE_SIZE = 80000;

    private final Map<String, Order> orderCache = new ConcurrentHashMap<>();
    private final Queue<String> cacheEvictionQueue = new ConcurrentLinkedQueue<>();
    private final LongAdder processedCount = new LongAdder();
    private final LongAdder stallCount = new LongAdder();
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    private volatile boolean burstMode = false;
    private volatile boolean running = true;

    public static void main(String[] args) {
        printHeader();
        new FlashSaleGCDemo().run();
    }

    private static void printHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         FLASH SALE GC DEMO - ALLOCATION STALL TEST           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        System.out.print("GC: ");
        java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
                .forEach(gc -> System.out.print(gc.getName() + " "));
        System.out.println("\n");
    }

    public void run() {
        ExecutorService workers = Executors.newFixedThreadPool(16);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (RUN_DURATION_SEC * 1000L);

        scheduler.scheduleAtFixedRate(this::triggerBurst, BURST_INTERVAL_SEC, BURST_INTERVAL_SEC, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::printMetrics, 5, 5, TimeUnit.SECONDS);

        System.out.println("Starting order processing...\n");

        while (running && System.currentTimeMillis() < endTime) {
            int targetRate = burstMode ? BURST_ORDERS_PER_SEC : BASE_ORDERS_PER_SEC;
            long intervalNanos = 1_000_000_000L / targetRate;
            long deadline = System.nanoTime() + intervalNanos;

            workers.submit(this::processOrder);

            while (System.nanoTime() < deadline && running && System.currentTimeMillis() < endTime) {
                Thread.onSpinWait();
            }
        }

        running = false;
        shutdown(workers, scheduler);
    }

    private void processOrder() {
        long start = System.nanoTime();

        Order order = new Order();
        for (int i = 0; i < 10 + (int)(Math.random() * 20); i++) {
            order.addItem(new OrderItem());
        }

        order.validate();
        order.calculateTotals();
        order.generateInvoice();

        if (Math.random() < 0.08) {
            cacheOrder(order);
        }

        if (Math.random() < 0.7) {
            order.processShipping();
        }

        generateAnalytics(order);

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        processedCount.increment();
        updateMaxLatency(latencyMs);

        if (latencyMs > 50) {
            stallCount.increment();
        }
    }

    private void cacheOrder(Order order) {
        orderCache.put(order.id, order);
        cacheEvictionQueue.add(order.id);
        while (orderCache.size() > CACHE_SIZE) {
            String evict = cacheEvictionQueue.poll();
            if (evict != null) {
                orderCache.remove(evict);
            } else {
                break;
            }
        }
    }

    private void generateAnalytics(Order order) {
        List<Map<String, Object>> events = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("orderId", order.id);
            event.put("timestamp", System.currentTimeMillis());
            event.put("payload", new byte[1024]);
            event.put("metrics", new byte[512]);
            events.add(event);
        }
    }

    private void updateMaxLatency(long latency) {
        long current;
        while (latency > (current = maxLatencyMs.get())) {
            if (maxLatencyMs.compareAndSet(current, latency)) break;
        }
    }

    private void triggerBurst() {
        if (!running) return;
        System.out.println(">>> FLASH SALE BURST! (" + BURST_ORDERS_PER_SEC + " orders/sec) <<<");
        burstMode = true;
        new Thread(() -> {
            try {
                Thread.sleep(BURST_DURATION_SEC * 1000L);
            } catch (InterruptedException ignored) {}
            burstMode = false;
            System.out.println(">>> Burst ended <<<\n");
        }).start();
    }

    private void printMetrics() {
        long max = maxLatencyMs.getAndSet(0);
        long stalls = stallCount.sumThenReset();
        long heap = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

        System.out.printf("[STATS] Processed: %,d | MaxLatency: %d ms | Stalls(>50ms): %d | Heap: %d MB | Cache: %d%n",
                processedCount.sum(), max, stalls, heap, orderCache.size());

        if (max > 100) {
            System.out.println("   ⚠️  HIGH LATENCY - potential allocation stall!");
        }
    }

    private void shutdown(ExecutorService workers, ScheduledExecutorService scheduler) {
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("SIMULATION COMPLETE");
        System.out.println("Total orders processed: " + processedCount.sum());
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("\nOpen the .jfr file in JMC and check:");
        System.out.println("  • Event Browser → ZAllocationStall events");
        System.out.println("  • JVM Internals → GC pause times");
        System.out.println("  • Memory → Heap usage patterns");

        workers.shutdownNow();
        scheduler.shutdownNow();
    }

    static class Order {
        final String id = UUID.randomUUID().toString();
        final List<OrderItem> items = new ArrayList<>();
        final byte[] metadata = new byte[4096];
        final byte[] auditLog = new byte[2048];
        final byte[] transactionData = new byte[1024];
        double subtotal, tax, total;
        String invoice;

        void addItem(OrderItem item) {
            items.add(item);
        }

        void validate() {
            StringBuilder log = new StringBuilder();
            for (OrderItem item : items) {
                log.append("Validated: ").append(item.sku).append("\n");
            }
        }

        void calculateTotals() {
            subtotal = items.stream().mapToDouble(i -> i.price * i.qty).sum();
            tax = subtotal * 0.08;
            total = subtotal + tax;
        }

        void generateInvoice() {
            StringBuilder sb = new StringBuilder(2048);
            sb.append("ORDER: ").append(id).append("\n");
            sb.append("TRANSACTION: ").append(UUID.randomUUID()).append("\n");
            for (OrderItem item : items) {
                sb.append(String.format("  %s x%d = $%.2f%n", item.sku, item.qty, item.price * item.qty));
            }
            sb.append(String.format("TOTAL: $%.2f%n", total));
            invoice = sb.toString();
        }

        void processShipping() {
            byte[] label = new byte[8192];
            byte[] customs = new byte[4096];
            String tracking = UUID.randomUUID().toString();
        }
    }

    static class OrderItem {
        final String sku = "SKU-" + (int)(Math.random() * 100000);
        final String name = "Product-" + UUID.randomUUID().toString().substring(0, 8);
        final String description = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        final int qty = 1 + (int)(Math.random() * 5);
        final double price = 10 + Math.random() * 200;
        final byte[] thumbnail = new byte[2048];
        final byte[] productData = new byte[1024];
    }
}
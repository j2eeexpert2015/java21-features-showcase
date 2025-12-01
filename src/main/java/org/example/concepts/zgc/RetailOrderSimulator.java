/**
 * RetailOrderSimulator.java
 *
 * Demonstrates G1GC vs Classic ZGC vs Generational ZGC under high allocation pressure.
 *
 * Expected results (JDK 21+):
 * ┌─────────────────────┬──────────────────────┬──────────────────────┬─────────────────────────────┐
 * │ GC                  │ Pause behaviour      │ Allocation rate      │ ZAllocationStall events     │
 * ├─────────────────────┼──────────────────────┼──────────────────────┼─────────────────────────────┤
 * │ G1GC                │ 100–800 ms STW       │ Drops to zero        │ 0                           │
 * │ Classic ZGC         │ <10 ms pauses        │ Drops during stalls  │ 200–400                     │
 * │ Generational ZGC    │ <1 ms minor GC       │ Steady throughout    │ 0                           │
 * └─────────────────────┴──────────────────────┴──────────────────────┴─────────────────────────────┘
 *
 * SINGLE-LINE COMMANDS (JDK 21+) — no deprecated flags:
 *
 * 1. G1GC
 * java -XX:+UseG1GC -Xmx2g -Xms2g -Xlog:gc*,gc+heap=debug:file=g1.log -XX:StartFlightRecording=duration=90s,settings=profile,filename=g1.jfr -cp target/classes org.example.concepts.zgc.RetailOrderSimulator
 *
 * 2. Classic ZGC
 * java -XX:+UseZGC -Xmx2g -Xms2g -Xlog:gc*,gc+heap=debug:file=zgc-non-gen.log -XX:StartFlightRecording=duration=90s,settings=profile,filename=zgc-non-gen.jfr -cp target/classes org.example.concepts.zgc.RetailOrderSimulator
 *
 * 3. Generational ZGC — zero allocation stalls (recommended production tuning)
 * java -XX:+UseZGC -XX:+ZGenerational -Xmx2g -Xms2g -XX:+AlwaysPreTouch -XX:ConcGCThreads=2 -XX:ParallelGCThreads=2 -XX:ZCollectionInterval=1 -XX:ZAllocationSpikeTolerance=4 -XX:+UnlockExperimentalVMOptions -XX:+ZProactive -XX:+ZUncommit -Xlog:gc*,gc+heap=debug:file=gen-zgc.log -XX:StartFlightRecording=duration=90s,settings=profile,filename=gen-zgc.jfr -cp target/classes org.example.concepts.zgc.RetailOrderSimulator
 *
 * Verification in Java Mission Control:
 *   • Event Browser → ZAllocationStall → Count = 0 only with Generational ZGC
 *   • Java Application → Allocation graph → steady line only with Generational ZGC
 *   • GC timeline → clean minor collections, no yellow stall bars
 */

package org.example.concepts.zgc;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RetailOrderSimulator {

    private static final List<Customer> LIVE_CUSTOMERS = new ArrayList<>();
    private static final int LIVE_SET_SIZE = 200_000; // ~200 MB long-lived data

    static class Customer {
        final String id = "CUST-" + System.nanoTime();
        final String name;
        final List<String> history = Arrays.asList("2020", "2021", "2022", "2023", "2024");

        Customer(int index) { this.name = "Customer-" + index; }
    }

    static class Item {
        final String sku = "SKU-" + ThreadLocalRandom.current().nextInt(1_000_000);
        final double price = ThreadLocalRandom.current().nextDouble(5, 999.99);
        final int qty = ThreadLocalRandom.current().nextInt(1, 20);
        final byte[] payload = new byte[80];
    }

    static class Order {
        final long orderId = System.nanoTime();
        final Customer customer;
        final List<Item> items = new ArrayList<>();
        final long timestamp = System.currentTimeMillis();

        Order(Customer c, int itemCount) {
            this.customer = c;
            for (int i = 0; i < itemCount; i++) {
                items.add(new Item());
                items.add(new Item());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== G1GC vs Classic ZGC vs Generational ZGC Demo ===");
        System.out.println("Heap: 2 GB | Live data: ~200 MB | 60-second high-allocation workload");
        Thread.sleep(3000);

        for (int i = 0; i < LIVE_SET_SIZE; i++) {
            LIVE_CUSTOMERS.add(new Customer(i));
        }
        System.out.println("Created " + LIVE_SET_SIZE + " long-lived Customer objects");

        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Starting " + threads + " allocator threads...");

        long start = System.nanoTime();
        List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                long orders = 0;
                Random rnd = ThreadLocalRandom.current();
                while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(60)) {
                    Customer c = LIVE_CUSTOMERS.get(rnd.nextInt(LIVE_CUSTOMERS.size()));
                    new Order(c, 15 + rnd.nextInt(45));
                    orders++;
                    if (orders % 20_000 == 0) Thread.onSpinWait();
                }
                System.out.println(Thread.currentThread().getName() + " created " + orders + " orders");
            }, "Worker-" + i);
            workers.add(t);
            t.start();
        }

        for (Thread t : workers) t.join();

        System.out.println("\nRun complete. Open the generated .jfr files in Java Mission Control.");
    }
}
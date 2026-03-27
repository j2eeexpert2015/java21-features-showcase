package org.example.concepts.unnamed;

import java.util.List;

/**
 * Demonstrates: Unnamed Variables (JEP 443 / JEP 456)
 *
 * Unnamed variables use _ in contexts where you are DECLARING a variable
 * but have no intention of using its value.
 *
 * Contexts covered:
 *   1. Exception catch blocks
 *   2. Lambda parameters (single param)
 *   3. Enhanced for loops
 *   4. Try-with-resources
 *
 */
public class UnnamedVariablesDemo {

    // 1. EXCEPTION HANDLING

    // BEFORE: 'ex' is declared but never used — compiler/IDE warning noise
    public String reserveStock_Before(String productId, int quantity) {
        try {
            performReservation(productId, quantity);
            return "Reserved successfully";
        } catch (IllegalArgumentException ex) {   // 'ex' unused
            return "Insufficient stock available";
        }
    }

    // AFTER: _ signals intent — we only care about the exception type
    public String reserveStock_After(String productId, int quantity) {
        try {
            performReservation(productId, quantity);
            return "Reserved successfully";
        } catch (IllegalArgumentException _) {    // unnamed variable
            return "Insufficient stock available";
        }
    }

    private void performReservation(String productId, int quantity) {
        if (quantity > 100) {
            throw new IllegalArgumentException("Stock limit exceeded for: " + productId);
        }
        System.out.println("Reserved " + quantity + " units of " + productId);
    }

    // 2. LAMBDA PARAMETERS

    public void lambdaDemo() {
        record OrderEvent(String orderId, String type) {}

        var events = List.of(
                new OrderEvent("O1", "COMPLETED"),
                new OrderEvent("O2", "COMPLETED"),
                new OrderEvent("O3", "PENDING")
        );

        System.out.println("\n-- Lambda: single param --");

        // BEFORE: 'event' is declared but never read inside the body
        events.stream()
                .filter(e -> e.type().equals("COMPLETED"))
                .forEach(event -> System.out.println("  [before] order processed"));

        // AFTER: _ makes the intent explicit
        events.stream()
                .filter(e -> e.type().equals("COMPLETED"))
                .forEach(_ -> System.out.println("  [after]  order processed"));
    }

    // 3. ENHANCED FOR LOOP

    public void loopDemo() {
        System.out.println("\n-- Enhanced for loop --");

        var retrySlots = List.of(1, 2, 3);

        // BEFORE: 'attempt' declared, never used inside the body
        int count = 0;
        for (var attempt : retrySlots) {   // 'attempt' unused
            count++;
        }
        System.out.println("  [before] attempt count: " + count);

        // AFTER: _ removes the misleading name
        int retries = 0;
        for (var _ : retrySlots) {         // unnamed variable
            retries++;
        }
        System.out.println("  [after]  retry count:   " + retries);
    }

    // 4. TRY-WITH-RESOURCES

    // Simulates a lock — acquired and auto-closed, never referenced by name
    static class InventoryLock implements AutoCloseable {
        static InventoryLock acquire() {
            System.out.println("  Lock acquired");
            return new InventoryLock();
        }
        @Override
        public void close() {
            System.out.println("  Lock released automatically");
        }
    }

    public void tryWithResourcesDemo() {
        System.out.println("\n-- Try-with-resources --");

        // BEFORE: 'lock' declared, never referenced inside the block
        System.out.println("  [before]");
        try (var lock = InventoryLock.acquire()) {   // 'lock' unused
            System.out.println("  Updating inventory...");
        }

        // AFTER: resource is still acquired and auto-closed — just not named
        System.out.println("  [after]");
        try (var _ = InventoryLock.acquire()) {      // unnamed variable
            System.out.println("  Updating inventory...");
        }
    }

    // MAIN

    public static void main(String[] args) {
        var demo = new UnnamedVariablesDemo();

        System.out.println("=== 1. Exception Handling ===");
        System.out.println(demo.reserveStock_Before("P001", 200));
        System.out.println(demo.reserveStock_After("P001", 200));

        System.out.println("\n=== 2. Lambda Parameters ===");
        demo.lambdaDemo();

        System.out.println("\n=== 3. Enhanced For Loop ===");
        demo.loopDemo();

        System.out.println("\n=== 4. Try-with-Resources ===");
        demo.tryWithResourcesDemo();
    }
}

package org.example.concepts.unnamed;

import org.example.concepts.unnamed.model.Customer;
import org.example.concepts.unnamed.model.Order;
import org.example.concepts.unnamed.model.PaymentInfo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates: Unnamed Variables (JEP 443 / JEP 456)
 *
 * Unnamed variables use _ in contexts where you are DECLARING a variable
 * but have no intention of using its value.
 *
 * Contexts covered:
 *   1. Exception catch blocks
 *   2. Lambda parameters (single and two-param)
 *   3. Enhanced for loops
 *   4. Try-with-resources
 *
 * Compile: javac --enable-preview --source 21
 * Run:     java  --enable-preview
 */
public class UnnamedVariablesDemo {

    // ─────────────────────────────────────────────
    // 1. EXCEPTION HANDLING
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    // 2. LAMBDA PARAMETERS
    // ─────────────────────────────────────────────

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

        System.out.println("\n-- Lambda: two params (Comparator) --");

        var products = new ArrayList<>(List.of("Laptop", "Phone", "Tablet", "Mouse"));

        // BEFORE: a and b declared but neither is used — external service drives order
        products.sort((a, b) -> 0);
        System.out.println("  [before] " + products);

        // AFTER: each _ is an independent unnamed variable
        products.sort((_, _) -> 0);
        System.out.println("  [after]  " + products);
    }

    // ─────────────────────────────────────────────
    // 3. ENHANCED FOR LOOP
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    // 4. TRY-WITH-RESOURCES
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    // 5. SERVICE LAYER (Slide 10 — case study)
    // ─────────────────────────────────────────────

    public Order processOrder(String orderId) {
        try {
            return findOrder(orderId);
        } catch (IllegalStateException _) {    // unnamed variable — type is enough
            System.err.println("Database error processing order: " + orderId);
            throw new RuntimeException("Unable to process order");
        }
    }

    public void sendOrderConfirmation(Order order) {
        try {
            sendEmail(order.customer().email(), "Confirmation for " + order.orderId());
        } catch (RuntimeException _) {         // unnamed variable — log and continue
            System.err.println("Failed to send confirmation for: " + order.orderId());
        }
    }

    private Order findOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalStateException("DB connection error");
        }
        return new Order(
            orderId,
            new Customer("C1", "alice@example.com", "PREMIUM"),
            new PaymentInfo("CARD", true),
            new BigDecimal("299.99")
        );
    }

    private void sendEmail(String to, String body) {
        if (to == null) throw new RuntimeException("Mail server unreachable");
        System.out.println("  Email sent to: " + to);
    }

    // ─────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────

    public static void main(String[] args) {
        var demo = new UnnamedVariablesDemo();

        System.out.println("=== 1. Exception Handling ===");
        System.out.println(demo.reserveStock_Before("P001", 200));
        System.out.println(demo.reserveStock_After("P001", 200));

        System.out.println("\n=== 2 & 3. Lambda and Loop ===");
        demo.lambdaDemo();
        demo.loopDemo();

        System.out.println("\n=== 4. Try-with-Resources ===");
        demo.tryWithResourcesDemo();

        System.out.println("\n=== 5. Service Layer ===");
        var order = demo.processOrder("O999");
        demo.sendOrderConfirmation(order);
        System.out.println("  Triggering catch block:");
        try {
            demo.processOrder(null);
        } catch (RuntimeException e) {
            System.out.println("  Caught: " + e.getMessage());
        }
    }
}

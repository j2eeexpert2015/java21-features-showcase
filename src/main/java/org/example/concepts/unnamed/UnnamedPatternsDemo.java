package org.example.concepts.unnamed;

import org.example.concepts.unnamed.model.Customer;
import org.example.concepts.unnamed.model.Order;
import org.example.concepts.unnamed.model.PaymentInfo;

import java.math.BigDecimal;

/**
 * Demonstrates: Unnamed Patterns (JEP 443 / JEP 456)
 *
 * Unnamed patterns use _ in contexts where you are MATCHING a value
 * via pattern matching but do not need to bind it to a variable.
 *
 * Contexts covered:
 *   1. instanceof with record patterns — before/after comparison
 *   2. switch expressions with record patterns — discount rule logic
 *
 * Note: Java 21 record patterns do not support constant patterns (string literals,
 * enum constants) as nested components. Use a when guard for constant matching.
 *
 * Compile: javac --enable-preview --source 21
 * Run:     java  --enable-preview
 */
public class UnnamedPatternsDemo {

    // ─────────────────────────────────────────────
    // 1. instanceof — RECORD DESTRUCTURING
    // ─────────────────────────────────────────────

    // BEFORE: all components bound even though only 'email' is used
    public String getCustomerEmail_Before(Order order) {
        if (order instanceof Order(
                String id,
                Customer(String custId, String email, String tier),
                PaymentInfo payment,
                var total)) {
            // id, custId, tier, payment, total — all unused noise
            return email;
        }
        return null;
    }

    // AFTER: _ replaces every component we don't need
    public String getCustomerEmail_After(Order order) {
        if (order instanceof Order(
                _,
                Customer(_, String email, _),
                _,
                _)) {
            return email;
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // 2. switch — DISCOUNT CALCULATION
    // ─────────────────────────────────────────────

    public BigDecimal calculateDiscount(Order order) {
        return switch (order) {

            // PREMIUM: bind tier with when guard — only total extracted
            // constant patterns ("PREMIUM") are not supported in Java 21 record patterns
            case Order(_, Customer(_, _, var tier), _, var total)
                    when tier.equals("PREMIUM") ->
                    total.multiply(new BigDecimal("0.20"));

            // REGULAR: same approach
            case Order(_, Customer(_, _, var tier), _, var total)
                    when tier.equals("REGULAR") ->
                    total.multiply(new BigDecimal("0.10"));

            // Default: no discount — no component needed at all
            case Order(_, Customer(_, _, _), _, _) ->
                    BigDecimal.ZERO;
        };
    }

    public boolean requiresManagerApproval(Order order) {
        return switch (order) {
            // Only total is relevant — everything else skipped
            case Order(_, _, _, BigDecimal total)
                    when total.compareTo(new BigDecimal("1000")) > 0 -> true;

            case Order(_, _, _, _) -> false;
        };
    }

    // ─────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────

    public static void main(String[] args) {
        var demo = new UnnamedPatternsDemo();

        var premiumOrder = new Order(
                "O001",
                new Customer("C1", "alice@example.com", "PREMIUM"),
                new PaymentInfo("CARD", true),
                new BigDecimal("500.00")
        );

        var regularOrder = new Order(
                "O002",
                new Customer("C2", "bob@example.com", "REGULAR"),
                new PaymentInfo("UPI", true),
                new BigDecimal("1200.00")
        );

        var newOrder = new Order(
                "O003",
                new Customer("C3", "carol@example.com", "NEW"),
                new PaymentInfo("CASH", false),
                new BigDecimal("80.00")
        );

        System.out.println("=== instanceof — getCustomerEmail ===");
        System.out.println("Before: " + demo.getCustomerEmail_Before(premiumOrder));
        System.out.println("After:  " + demo.getCustomerEmail_After(premiumOrder));

        System.out.println("\n=== switch — calculateDiscount ===");
        System.out.println("PREMIUM discount: " + demo.calculateDiscount(premiumOrder));
        System.out.println("REGULAR discount: " + demo.calculateDiscount(regularOrder));
        System.out.println("NEW     discount: " + demo.calculateDiscount(newOrder));

        System.out.println("\n=== switch — requiresManagerApproval ===");
        System.out.println("PREMIUM order ($500):  " + demo.requiresManagerApproval(premiumOrder));
        System.out.println("REGULAR order ($1200): " + demo.requiresManagerApproval(regularOrder));
    }
}

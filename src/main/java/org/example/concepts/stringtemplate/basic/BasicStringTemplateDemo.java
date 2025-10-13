package org.example.concepts.stringtemplate.basic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Basic String Template Demonstrations - Java 21
 *
 * String Templates (Preview Feature in Java 21)
 * - STR processor for simple interpolation
 * - Expression embedding with \{...}
 * - Compile-time safety
 * - Multi-line support
 *
 * Enable preview features:
 * --enable-preview --release 21
 */
public class BasicStringTemplateDemo {

    public static void main(String[] args) {
        System.out.println("=== Basic String Template Demo ===\n");

        demonstrateSTRProcessor();
        demonstrateExpressions();
        demonstrateMultiLine();
        demonstrateOldVsNewWay();
    }

    /**
     * 1️⃣ STR Processor - Simple String Interpolation
     *
     * STR is the standard template processor that performs
     * string interpolation with embedded expressions.
     */
    private static void demonstrateSTRProcessor() {
        System.out.println("1. STR Processor - Basic Interpolation:");

        String name = "John Doe";
        int age = 30;
        String city = "San Francisco";

        // ❌ OLD WAY: String concatenation
        String oldWay = "Name: " + name + ", Age: " + age + ", City: " + city;
        System.out.println("OLD: " + oldWay);

        // ✅ NEW WAY: String Template with STR processor
        String newWay = STR."Name: \{name}, Age: \{age}, City: \{city}";
        System.out.println("NEW: " + newWay);

        System.out.println();
    }

    /**
     * 2️⃣ Expression Embedding - Complex Expressions
     *
     * String templates can embed ANY Java expression inside \{...}
     * - Method calls
     * - Arithmetic operations
     * - Ternary operators
     * - Object property access
     */
    private static void demonstrateExpressions() {
        System.out.println("2. Expression Embedding:");

        String firstName = "John";
        String lastName = "Doe";
        int items = 5;
        double price = 29.99;
        boolean isPremium = true;

        // ❌ OLD WAY: Complex concatenation
        String oldMessage = "Customer: " + firstName.toUpperCase() + " " + lastName.toUpperCase() +
                ", Total: $" + String.format("%.2f", items * price) +
                ", Status: " + (isPremium ? "PREMIUM" : "STANDARD");
        System.out.println("OLD: " + oldMessage);

        // ✅ NEW WAY: Expressions embedded directly
        String newMessage = STR."""
            Customer: \{firstName.toUpperCase()} \{lastName.toUpperCase()}
            Total: $\{String.format("%.2f", items * price)}
            Status: \{isPremium ? "PREMIUM" : "STANDARD"}
            """;
        System.out.println("NEW:");
        System.out.println(newMessage);
    }

    /**
     * 3️⃣ Multi-Line Templates - Text Blocks with Interpolation
     *
     * Combines Java 13 Text Blocks with Java 21 String Templates
     * Perfect for:
     * - Email templates
     * - JSON/XML generation
     * - SQL queries
     * - HTML content
     */
    private static void demonstrateMultiLine() {
        System.out.println("3. Multi-Line String Templates:");

        String customerName = "Alice Johnson";
        String orderId = "ORD-2024-12345";
        double amount = 1299.99;
        LocalDate orderDate = LocalDate.now();
        String formattedDate = orderDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

        // ❌ OLD WAY: StringBuilder or multiple concatenations
        StringBuilder oldEmail = new StringBuilder();
        oldEmail.append("Dear ").append(customerName).append(",\n\n");
        oldEmail.append("Your order #").append(orderId).append(" has been confirmed!\n\n");
        oldEmail.append("Order Summary:\n");
        oldEmail.append("- Order Date: ").append(formattedDate).append("\n");
        oldEmail.append("- Total Amount: $").append(String.format("%.2f", amount)).append("\n\n");
        oldEmail.append("Thank you for shopping with us!");

        System.out.println("OLD WAY (StringBuilder):");
        System.out.println(oldEmail);
        System.out.println();

        // ✅ NEW WAY: Multi-line string template
        String newEmail = STR."""
            Dear \{customerName},

            Your order #\{orderId} has been confirmed!

            Order Summary:
            - Order Date: \{formattedDate}
            - Total Amount: $\{String.format("%.2f", amount)}

            Thank you for shopping with us!
            """;

        System.out.println("NEW WAY (String Template):");
        System.out.println(newEmail);
    }

    /**
     * 4️⃣ Comprehensive Comparison: OLD vs NEW
     *
     * Shows side-by-side comparison of different approaches
     */
    private static void demonstrateOldVsNewWay() {
        System.out.println("\n4. OLD vs NEW - Complete Comparison:");

        String product = "MacBook Pro";
        int quantity = 2;
        double unitPrice = 2499.00;
        double discount = 0.10; // 10%

        System.out.println("Scenario: Generate invoice line");
        System.out.println();

        // ❌ Method 1: String concatenation
        long start1 = System.nanoTime();
        String concat = "Product: " + product + " | Qty: " + quantity +
                " | Unit Price: $" + unitPrice +
                " | Discount: " + (discount * 100) + "%" +
                " | Total: $" + String.format("%.2f", quantity * unitPrice * (1 - discount));
        long end1 = System.nanoTime();
        System.out.println("Method 1 - Concatenation:");
        System.out.println(concat);
        System.out.println("Time: " + (end1 - start1) + " ns");
        System.out.println();

        // ❌ Method 2: String.format()
        long start2 = System.nanoTime();
        String formatted = String.format(
                "Product: %s | Qty: %d | Unit Price: $%.2f | Discount: %.0f%% | Total: $%.2f",
                product, quantity, unitPrice, discount * 100, quantity * unitPrice * (1 - discount)
        );
        long end2 = System.nanoTime();
        System.out.println("Method 2 - String.format():");
        System.out.println(formatted);
        System.out.println("Time: " + (end2 - start2) + " ns");
        System.out.println();

        // ✅ Method 3: String Template (Java 21)
        long start3 = System.nanoTime();
        String template = STR."""
            Product: \{product} | Qty: \{quantity} | Unit Price: $\{unitPrice} | \
            Discount: \{discount * 100}% | Total: $\{String.format("%.2f", quantity * unitPrice * (1 - discount))}""";
        long end3 = System.nanoTime();
        System.out.println("Method 3 - String Template (Java 21):");
        System.out.println(template);
        System.out.println("Time: " + (end3 - start3) + " ns");
        System.out.println();

        System.out.println("Benefits of String Templates:");
        System.out.println("✅ More readable - values close to usage");
        System.out.println("✅ Type-safe - compile-time checking");
        System.out.println("✅ Fewer errors - no format specifier mismatches");
        System.out.println("✅ Maintainable - easy to see what goes where");
    }
}
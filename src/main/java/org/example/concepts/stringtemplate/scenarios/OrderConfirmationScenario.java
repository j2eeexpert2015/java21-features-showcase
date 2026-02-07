package org.example.concepts.stringtemplate.scenarios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case 1: Order Confirmation Email Demo
 *
 * Demonstrates String Templates for generating professional order confirmation emails.
 * Shows the "Compute First, Embed Result" pattern with OLD WAY vs NEW WAY comparison.
 *
 * This demo accompanies Slides 19-20 of the String Templates presentation.
 */
public class OrderConfirmationScenario {

    /**
     * Customer record - holds customer information
     */
    record Customer(String name, String email, String tier) {}

    /**
     * OrderItem record - represents a single item in an order
     * Includes a total() method that calculates item total (price * quantity)
     */
    record OrderItem(String productName, int quantity, BigDecimal price) {
        BigDecimal total() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Order record - represents a complete order
     * Contains calculation methods:
     * - subtotal(): sums all item totals
     * - tax(): calculates 8% tax on subtotal
     * - total(): combines subtotal and tax
     */
    record Order(
            String orderId,
            Customer customer,
            List<OrderItem> items,
            LocalDateTime orderDate,
            String shippingAddress
    ) {
        BigDecimal subtotal() {
            return items.stream()
                    .map(OrderItem::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal tax() {
            return subtotal().multiply(BigDecimal.valueOf(0.08)); // 8% tax
        }

        BigDecimal total() {
            return subtotal().add(tax());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Order Confirmation Email Demo ===\n");

        // Create sample customer
        Customer customer = new Customer(
                "Sarah Johnson",
                "sarah@email.com",
                "PREMIUM"
        );

        // Create sample order items
        List<OrderItem> items = List.of(
                new OrderItem("Laptop Pro", 1, new BigDecimal("1299.99")),
                new OrderItem("Wireless Mouse", 2, new BigDecimal("29.99"))
        );
        // Total: $1,375.96 (before tax)

        // Create order
        Order order = new Order(
                "ORD-1001",
                customer,
                items,
                LocalDateTime.now(),
                "123 Main St, San Francisco, CA 94105"
        );

        // Demonstrate OLD WAY vs NEW WAY
        demonstrateOrderConfirmation(order);
    }

    /**
     * Demonstrates Order Confirmation Email generation
     * Compares OLD WAY (StringBuilder) vs NEW WAY (String Templates)
     *
     * Shows the "Compute First, Embed Result" pattern:
     * Step 1: Pre-compute itemsList using streams
     * Step 2: Build email template with simple embedded expressions
     */
    private static void demonstrateOrderConfirmation(Order order) {
        System.out.println("ORDER CONFIRMATION EMAIL");
        System.out.println("━".repeat(60));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
        String formattedDate = order.orderDate().format(formatter);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ❌ OLD WAY - StringBuilder approach
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        StringBuilder oldEmail = new StringBuilder();
        oldEmail.append("Subject: Order Confirmation - ").append(order.orderId()).append("\n\n");
        oldEmail.append("Dear ").append(order.customer().name()).append(",\n\n");
        oldEmail.append("Thank you for your order!\n\n");
        oldEmail.append("Order Details:\n");
        oldEmail.append("Order Number: ").append(order.orderId()).append("\n");
        oldEmail.append("Order Date: ").append(formattedDate).append("\n");
        oldEmail.append("Customer Tier: ").append(order.customer().tier()).append("\n\n");

        oldEmail.append("Items Ordered:\n");
        for (OrderItem item : order.items()) {
            oldEmail.append("- ").append(item.productName())
                    .append(" (Qty: ").append(item.quantity()).append(")")
                    .append(" - $").append(item.price()).append("\n");
        }

        oldEmail.append("\nSubtotal: $").append(order.subtotal()).append("\n");
        oldEmail.append("Tax: $").append(order.tax()).append("\n");
        oldEmail.append("Total: $").append(order.total()).append("\n");

        System.out.println("OLD WAY (StringBuilder - 30+ lines):");
        System.out.println(oldEmail);
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ NEW WAY - String Template approach
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        // STEP 1: Extract complex logic - build itemsList separately
        // Stream through items, format each with String Template, join with newlines
        String itemsList = order.items().stream()
                .map(item -> STR."- \{item.productName()} (Qty: \{item.quantity()}) - $\{item.price()}")
                .collect(Collectors.joining("\n"));

        // STEP 2: Build email template with simple embedded expressions
        // Template stays clean - just embeds values, no computation
        String newEmail = STR."""
            Subject: Order Confirmation - \{order.orderId()}

            Dear \{order.customer().name()},

            Thank you for your order!

            Order Details:
            Order Number: \{order.orderId()}
            Order Date: \{formattedDate}
            Customer Tier: \{order.customer().tier()}

            Items Ordered:
            \{itemsList}

            Subtotal: $\{order.subtotal()}
            Tax: $\{order.tax()}
            Total: $\{order.total()}

            Your order will be processed within 24 hours.

            Best regards,
            TechMart Team
            """;

        System.out.println("NEW WAY (String Template - cleaner, safer):");
        System.out.println(newEmail);
        System.out.println();

    }
}
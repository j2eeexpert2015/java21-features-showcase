package org.example.concepts.stringtemplate.scenarios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Real-World Scenario: E-Commerce Email Notification System
 *
 * Demonstrates String Templates for:
 * - Order confirmations
 * - Shipping notifications
 * - Invoice generation
 * - Customer communication
 *
 * Shows how String Templates solve real business problems.
 */
public class EmailNotificationScenario {

    record Customer(String name, String email, String tier) {}

    record OrderItem(String productName, int quantity, BigDecimal price) {
        BigDecimal total() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

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
        System.out.println("=== Email Notification Scenario Demo ===\n");

        // Create sample order
        Customer customer = new Customer("Sarah Johnson", "sarah.j@email.com", "PREMIUM");

        List<OrderItem> items = List.of(
                new OrderItem("iPhone 15 Pro", 1, new BigDecimal("1199.00")),
                new OrderItem("AirPods Pro", 1, new BigDecimal("249.00")),
                new OrderItem("MagSafe Charger", 2, new BigDecimal("39.00"))
        );

        Order order = new Order(
                "ORD-2024-12345",
                customer,
                items,
                LocalDateTime.now(),
                "123 Main St, San Francisco, CA 94105"
        );

        // Generate different email types
        demonstrateOrderConfirmation(order);
        //demonstrateShippingNotification(order);
        //demonstrateInvoice(order);
    }

    /**
     * 1️⃣ Order Confirmation Email
     *
     * ❌ OLD WAY: String concatenation or StringBuilder
     * ✅ NEW WAY: String Template for clarity and safety
     */
    private static void demonstrateOrderConfirmation(Order order) {
        System.out.println("1. ORDER CONFIRMATION EMAIL");
        System.out.println("━".repeat(60));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
        String formattedDate = order.orderDate().format(formatter);

        // ❌ OLD WAY - StringBuilder approach
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

        // ✅ NEW WAY - String Template
        String itemsList = order.items().stream()
                .map(item -> STR."- \{item.productName()} (Qty: \{item.quantity()}) - $\{item.price()}")
                .reduce("", (a, b) -> a + b + "\n");

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

    /**
     * 2️⃣ Shipping Notification
     *
     * Shows dynamic content based on customer tier
     */
    private static void demonstrateShippingNotification(Order order) {
        System.out.println("2. SHIPPING NOTIFICATION");
        System.out.println("━".repeat(60));

        String trackingNumber = "1Z999AA10123456784";
        String carrier = "FedEx";
        String estimatedDelivery = "December 28, 2024";

        // ✅ String Template with conditional logic
        String tierMessage = switch (order.customer().tier()) {
            case "PREMIUM" -> "As a Premium member, you get FREE expedited shipping!";
            case "VIP" -> "As a VIP member, you get FREE next-day delivery!";
            default -> "Standard shipping applied.";
        };

        String shippingEmail = STR."""
            Subject: Your Order Has Shipped! - \{order.orderId()}

            Hi \{order.customer().name()},

            Great news! Your order #\{order.orderId()} has been shipped.

            \{tierMessage}

            Shipping Details:
            Carrier: \{carrier}
            Tracking Number: \{trackingNumber}
            Estimated Delivery: \{estimatedDelivery}

            Shipping Address:
            \{order.shippingAddress()}

            Track your package: https://tracking.com/\{trackingNumber}

            Thank you for shopping with us!
            TechMart Support Team
            """;

        System.out.println(shippingEmail);
        System.out.println();
    }

    /**
     * 3️⃣ Invoice Generation
     *
     * Professional invoice with calculations
     */
    private static void demonstrateInvoice(Order order) {
        System.out.println("3. INVOICE GENERATION");
        System.out.println("━".repeat(60));

        String invoiceNumber = STR."INV-\{order.orderId().substring(4)}";
        String invoiceDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        // Generate itemized list
        StringBuilder itemRows = new StringBuilder();
        for (OrderItem item : order.items()) {
            itemRows.append(STR."""
                \{item.productName().substring(0, Math.min(30, item.productName().length()))} \
                | \{item.quantity()} | $\{item.price()} | $\{item.total()}
                """).append("\n");
        }

        String invoice = STR."""
            ═══════════════════════════════════════════════════════════════
                                    INVOICE
            ═══════════════════════════════════════════════════════════════

            Invoice #: \{invoiceNumber}
            Date: \{invoiceDate}
            Order #: \{order.orderId()}

            BILL TO:
            \{order.customer().name()}
            \{order.customer().email()}
            \{order.shippingAddress()}

            ───────────────────────────────────────────────────────────────
            ITEM                          | QTY | PRICE  | TOTAL
            ───────────────────────────────────────────────────────────────
            \{itemRows}───────────────────────────────────────────────────────────────

                                            Subtotal: $\{order.subtotal()}
                                            Tax (8%): $\{order.tax()}
                                            ─────────────────────────────
                                            TOTAL:    $\{order.total()}

            ═══════════════════════════════════════════════════════════════
            Payment Method: Credit Card ending in ****1234
            Transaction ID: TXN-2024-987654

            Thank you for your business!
            For questions: support@techmart.com | 1-800-TECH-MART
            ═══════════════════════════════════════════════════════════════
            """;

        System.out.println(invoice);
    }
}
package org.example.concepts.stringtemplate.scenarios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Real-World Scenario: E-Commerce Email Notification System
 *
 * Demonstrates String Templates for:
 * - Shipping notifications
 * - Invoice generation
 *
 * Note: Order Confirmation demo moved to OrderConfirmationScenario.java
 * (used for Slides 19-20 demonstration)
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
        demonstrateShippingNotification(order);
        demonstrateInvoice(order);
    }

    /**
     * Shipping Notification (Used in Slides 13-15)
     *
     * Shows dynamic content based on customer tier using switch expressions
     */
    private static void demonstrateShippingNotification(Order order) {
        System.out.println("SHIPPING NOTIFICATION");
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
     * Invoice Generation
     *
     * Professional invoice with calculations and formatted layout
     */
    private static void demonstrateInvoice(Order order) {
        System.out.println("INVOICE GENERATION");
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
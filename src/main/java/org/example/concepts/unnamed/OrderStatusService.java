package org.example.concepts.unnamed;

import org.example.concepts.unnamed.model.OrderStatus;
import org.example.concepts.unnamed.model.Status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Case Study: Real-world unnamed patterns in a service class (Slide 6)
 *
 * Teaching point: each switch arm extracts only what its business rule needs.
 * Everything else is skipped with _.
 *
 * Key contrast:
 *   SHIPPED  — extracts timestamp (the date is part of the message)
 *   PENDING  — skips timestamp and notes (neither is relevant)
 *
 * Note: enum constants (e.g. Status.SHIPPED) cannot be used directly as
 * component patterns inside record patterns in Java 21. Use a when guard instead.
 *
 * Compile: javac --enable-preview --source 21
 * Run:     java  --enable-preview
 */
public class OrderStatusService {

    public String getStatusMessage(OrderStatus orderStatus) {
        return switch (orderStatus) {

            // SHIPPED: bind status + timestamp, skip notes with _
            // when guard matches the enum constant — _ still skips notes
            case OrderStatus(String id, var status, var shipped, _)
                    when status == Status.SHIPPED ->
                    String.format("Order %s shipped on %s",
                            id, shipped.format(DateTimeFormatter.ISO_LOCAL_DATE));

            // PENDING: bind status + id, skip timestamp and notes with _
            case OrderStatus(String id, var status, _, _)
                    when status == Status.PENDING ->
                    "Order " + id + " is awaiting confirmation";

            // Catch-all: CONFIRMED, DELIVERED, any future statuses
            case OrderStatus(String id, var currentStatus, _, _) ->
                    String.format("Order %s status: %s", id, currentStatus);
        };
    }

    // ─────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────

    public static void main(String[] args) {
        var service = new OrderStatusService();
        var now = LocalDateTime.now();

        System.out.println("=== Order Status Messages ===\n");

        // SHIPPED — timestamp extracted, notes skipped
        System.out.println(service.getStatusMessage(
                new OrderStatus("O001", Status.SHIPPED, now, "Via FedEx")));

        // PENDING — timestamp and notes both skipped
        System.out.println(service.getStatusMessage(
                new OrderStatus("O002", Status.PENDING, now, null)));

        // DELIVERED — hits catch-all
        System.out.println(service.getStatusMessage(
                new OrderStatus("O003", Status.DELIVERED, now, null)));
    }
}

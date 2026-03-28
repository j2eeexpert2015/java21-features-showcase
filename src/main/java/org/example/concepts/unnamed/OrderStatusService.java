package org.example.concepts.unnamed;

import org.example.concepts.unnamed.model.OrderStatus;
import org.example.concepts.unnamed.model.Status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Case Study: Real-world unnamed patterns in a service class.
 *
 * Teaching point: each switch arm extracts only what its business rule needs.
 * Everything else is skipped with _.
 *
 * Key contrast:
 *   SHIPPED  — extracts timestamp (the date is part of the message)
 *   PENDING  — skips timestamp and notes (neither is relevant)
 */
public class OrderStatusService {

    public String getStatusMessage(OrderStatus orderStatus) {
        return switch (orderStatus) {

            // SHIPPED: timestamp needed — notes skipped with _
            case OrderStatus(String id, var status, var timestamp, _)
                    when status == Status.SHIPPED ->
                    String.format("Order %s shipped on %s",
                            id, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE));

            // PENDING: only id needed — timestamp and notes skipped with _
            case OrderStatus(String id, var status, _, _)
                    when status == Status.PENDING ->
                    "Order " + id + " is awaiting confirmation";

            // Catch-all: CONFIRMED, DELIVERED, any future statuses
            case OrderStatus(String id, var currentStatus, _, _) ->
                    String.format("Order %s status: %s", id, currentStatus);
        };
    }

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

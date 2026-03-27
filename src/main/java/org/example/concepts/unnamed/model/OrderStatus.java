package org.example.concepts.unnamed.model;

import java.time.LocalDateTime;

public record OrderStatus(
    String orderId,
    Status status,
    LocalDateTime timestamp,
    String notes
) {}

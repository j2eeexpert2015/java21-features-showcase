package org.example.concepts.unnamed.model;

public record Customer(
    String customerId,
    String email,
    String tier   // "PREMIUM", "REGULAR", "NEW"
) {}

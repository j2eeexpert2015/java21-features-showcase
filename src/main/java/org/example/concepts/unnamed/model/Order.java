package org.example.concepts.unnamed.model;

import java.math.BigDecimal;

public record Order(
    String orderId,
    Customer customer,
    PaymentInfo payment,
    BigDecimal total
) {}

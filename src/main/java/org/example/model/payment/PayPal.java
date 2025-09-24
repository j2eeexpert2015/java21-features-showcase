package org.example.model.payment;

import java.math.BigDecimal;

public record PayPal(
        String email,
        String accountId,
        BigDecimal amount,
        Long customerId
) implements Payment {

    public PayPal {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public BigDecimal getAmount() { return amount; }

    @Override
    public Long getCustomerId() { return customerId; }
}


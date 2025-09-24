package org.example.model.payment;


import java.math.BigDecimal;

// Record for pattern matching with destructuring
public record CreditCard(
        String cardNumber,
        String cardType,
        String cvv,
        String expiryDate,
        BigDecimal amount,
        Long customerId
) implements Payment {

    public CreditCard {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public BigDecimal getAmount() { return amount; }

    @Override
    public Long getCustomerId() { return customerId; }
}

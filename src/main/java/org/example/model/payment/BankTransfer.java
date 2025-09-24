package org.example.model.payment;

import java.math.BigDecimal;

public record BankTransfer(
        String routingNumber,
        String accountNumber,
        String bankName,
        BigDecimal amount,
        Long customerId
) implements Payment {

    public BankTransfer {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    @Override
    public BigDecimal getAmount() { return amount; }

    @Override
    public Long getCustomerId() { return customerId; }
}


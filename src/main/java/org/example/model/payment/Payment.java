package org.example.model.payment;

import java.math.BigDecimal;

// Sealed interface for exhaustive pattern matching
public sealed interface Payment permits CreditCard, PayPal, BankTransfer {
    BigDecimal getAmount();
    Long getCustomerId();
}

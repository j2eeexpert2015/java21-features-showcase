package org.example.dto.payment;

import org.example.model.payment.*;
import java.math.BigDecimal;

public class PaymentRequest {
    private String paymentMethod;
    private BigDecimal amount;
    private Long customerId;
    private CustomerType customerType;
    private boolean isInternational;

    public PaymentRequest() {}

    // Convert to Payment for pattern matching
    public Payment toPayment() {
        return switch (paymentMethod.toLowerCase()) {
            case "credit" -> new CreditCard("4532-1234-5678-9012", "Visa", "123", "12/25", amount, customerId);
            case "paypal" -> new PayPal("demo@example.com", "PP-123", amount, customerId);
            case "bank" -> new BankTransfer("123456789", "9876543210", "Demo Bank", amount, customerId);
            default -> throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
        };
    }

    // Getters and setters
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }
    public boolean isInternational() { return isInternational; }
    public void setInternational(boolean international) { isInternational = international; }
}


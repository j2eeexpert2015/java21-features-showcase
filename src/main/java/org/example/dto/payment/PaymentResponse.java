package org.example.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal processedAmount;
    private String paymentMethod;
    private LocalDateTime processedAt;
    private String validationMessage;
    private boolean requiresAdditionalVerification;

    public enum PaymentStatus {
        SUCCESS, PENDING, REQUIRES_VERIFICATION
    }

    public PaymentResponse() {}

    public PaymentResponse(String transactionId, PaymentStatus status, BigDecimal processedAmount, String paymentMethod) {
        this.transactionId = transactionId;
        this.status = status;
        this.processedAmount = processedAmount;
        this.paymentMethod = paymentMethod;
        this.processedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public BigDecimal getProcessedAmount() { return processedAmount; }
    public void setProcessedAmount(BigDecimal processedAmount) { this.processedAmount = processedAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getValidationMessage() { return validationMessage; }
    public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
    public boolean isRequiresAdditionalVerification() { return requiresAdditionalVerification; }
    public void setRequiresAdditionalVerification(boolean requiresAdditionalVerification) {
        this.requiresAdditionalVerification = requiresAdditionalVerification;
    }
}

package org.example.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for payment processing operations.
 * Contains transaction details and pattern matching execution steps.
 */
public class PaymentResponse {

    // Existing fields - unchanged
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal processedAmount;
    private String paymentMethod;
    private LocalDateTime processedAt;
    private String validationMessage;
    private boolean requiresAdditionalVerification;

    // NEW: Pattern matching execution steps for educational demonstration
    private List<PatternMatchingStep> patternMatchingSteps;

    public enum PaymentStatus {
        SUCCESS,
        PENDING,
        REQUIRES_VERIFICATION
    }

    // Constructors

    public PaymentResponse() {
        this.patternMatchingSteps = new ArrayList<>();
    }

    public PaymentResponse(String transactionId, PaymentStatus status,
                           BigDecimal processedAmount, String paymentMethod) {
        this.transactionId = transactionId;
        this.status = status;
        this.processedAmount = processedAmount;
        this.paymentMethod = paymentMethod;
        this.processedAt = LocalDateTime.now();
        this.patternMatchingSteps = new ArrayList<>();
    }

    // Existing getters and setters - unchanged

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getProcessedAmount() {
        return processedAmount;
    }

    public void setProcessedAmount(BigDecimal processedAmount) {
        this.processedAmount = processedAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public boolean isRequiresAdditionalVerification() {
        return requiresAdditionalVerification;
    }

    public void setRequiresAdditionalVerification(boolean requiresAdditionalVerification) {
        this.requiresAdditionalVerification = requiresAdditionalVerification;
    }

    // NEW: Pattern matching steps getters and setters

    /**
     * Get the list of pattern matching execution steps
     */
    public List<PatternMatchingStep> getPatternMatchingSteps() {
        return patternMatchingSteps;
    }

    /**
     * Set the list of pattern matching execution steps
     */
    public void setPatternMatchingSteps(List<PatternMatchingStep> patternMatchingSteps) {
        this.patternMatchingSteps = patternMatchingSteps != null ? patternMatchingSteps : new ArrayList<>();
    }

    /**
     * Add a single pattern matching step
     */
    public void addPatternMatchingStep(PatternMatchingStep step) {
        if (this.patternMatchingSteps == null) {
            this.patternMatchingSteps = new ArrayList<>();
        }
        this.patternMatchingSteps.add(step);
    }
}
// src/main/java/org/example/service/PaymentService.java
package org.example.service;

import org.example.dto.payment.PaymentResponse;
import org.example.model.payment.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal HIGH_VALUE = new BigDecimal("1000");
    private static final BigDecimal VERY_HIGH_VALUE = new BigDecimal("5000");

    /**
     * Main Java 21 pattern matching demonstration with real-time execution tracking
     */
    public PaymentResponse processPayment(Payment payment, CustomerType customerType, boolean isInternational) {
        logger.info("=== PAYMENT PROCESSING START ===");
        logger.info("Payment Type: {}", payment.getClass().getSimpleName());
        logger.info("Amount: ${}", payment.getAmount());
        logger.info("Customer: {} ({})", customerType.getDisplayName(), customerType.getPriority());
        logger.info("International: {}", isInternational);
        logger.info("Starting Java 21 pattern matching...");

        // Create tracker to record execution steps
        PatternMatchingTracker tracker = new PatternMatchingTracker();

        // Java 21 Pattern Matching with Sealed Interface
        PaymentResponse response = switch (payment) {

            // ═══════════════════════════════════════════════════════════════════
            // CASE 1: High-value international credit card
            // ═══════════════════════════════════════════════════════════════════
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var customerId)
                    when amount.compareTo(HIGH_VALUE) > 0 && isInternational -> {

                logger.info("✓ PATTERN MATCHED: CreditCard with high-value + international guards");
                logger.info("✓ DESTRUCTURED: type={}, amount=${}", type, amount);
                logger.info("✓ GUARD CONDITIONS: amount > 1000 AND international = true");

                // Track execution steps
                tracker.recordTypeCheck("CreditCard");
                tracker.recordDestructuringCreditCard(type, amount, expiry);
                tracker.recordGuardEvaluation(1, amount, HIGH_VALUE, isInternational, true);

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.REQUIRES_VERIFICATION,
                        amount,
                        "Credit Card",
                        "High-value international transaction requires verification"
                );
                resp.setRequiresAdditionalVerification(true);
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }

            // ═══════════════════════════════════════════════════════════════════
            // CASE 2: High-value domestic credit card
            // ═══════════════════════════════════════════════════════════════════
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var customerId)
                    when amount.compareTo(HIGH_VALUE) > 0 -> {

                logger.info("✓ PATTERN MATCHED: CreditCard with high-value guard");
                logger.info("✓ DESTRUCTURED: type={}, amount=${}", type, amount);
                logger.info("✓ GUARD CONDITION: amount > 1000");

                // Track execution - if we're here, Case 1 guard must have failed
                if (!tracker.hasSteps()) {
                    tracker.recordTypeCheck("CreditCard");
                    tracker.recordDestructuringCreditCard(type, amount, expiry);
                    // Case 1 guard failed (international was false)
                    tracker.recordGuardEvaluation(1, amount, HIGH_VALUE, isInternational, false);
                }

                // Case 2 guard evaluation (this one passed)
                tracker.recordGuardEvaluationAmountOnly(2, amount, HIGH_VALUE);

                String message = customerType == CustomerType.VIP
                        ? "VIP high-value transaction with express processing"
                        : "High-value transaction with enhanced verification";

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.SUCCESS,
                        amount,
                        "Credit Card",
                        message
                );
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }

            // ═══════════════════════════════════════════════════════════════════
            // CASE 3: Standard credit card (no guards)
            // ═══════════════════════════════════════════════════════════════════
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var customerId) -> {
                logger.info("✓ PATTERN MATCHED: CreditCard - standard processing");
                logger.info("✓ DESTRUCTURED: type={}, amount=${}", type, amount);

                // Track execution - if no steps yet, this is first case checked
                if (!tracker.hasSteps()) {
                    tracker.recordTypeCheck("CreditCard");
                    tracker.recordDestructuringCreditCard(type, amount, expiry);
                }
                // Note: If we reached here after Case 2, guards already tracked

                String message = isInternational
                        ? "International credit card processed"
                        : "Credit card processed successfully";

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.SUCCESS,
                        amount,
                        "Credit Card",
                        message
                );
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }

            // ═══════════════════════════════════════════════════════════════════
            // CASE 4: International PayPal
            // ═══════════════════════════════════════════════════════════════════
            case PayPal(var email, var accountId, var amount, var customerId) when isInternational -> {
                logger.info("✓ PATTERN MATCHED: PayPal with international guard");
                logger.info("✓ DESTRUCTURED: email={}, amount=${}", maskEmail(email), amount);
                logger.info("✓ GUARD CONDITION: international = true");

                // Track execution steps
                tracker.recordTypeCheck("PayPal");
                tracker.recordDestructuringPayPal(email, amount);
                tracker.recordGuardEvaluationInternationalOnly(1, isInternational);

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.SUCCESS,
                        amount,
                        "PayPal",
                        "International PayPal with currency conversion"
                );
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }

            // ═══════════════════════════════════════════════════════════════════
            // CASE 5: Standard PayPal (no guards)
            // ═══════════════════════════════════════════════════════════════════
            case PayPal(var email, var accountId, var amount, var customerId) -> {
                logger.info("✓ PATTERN MATCHED: PayPal - standard processing");
                logger.info("✓ DESTRUCTURED: email={}, amount=${}", maskEmail(email), amount);

                // Track execution
                if (!tracker.hasSteps()) {
                    tracker.recordTypeCheck("PayPal");
                    tracker.recordDestructuringPayPal(email, amount);
                }
                // If we reached here from Case 4, international guard failed
                // (but we don't track failed guards for non-international standard processing)

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.SUCCESS,
                        amount,
                        "PayPal",
                        "PayPal processed successfully"
                );
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }

            // ═══════════════════════════════════════════════════════════════════
            // CASE 6: Very high-value bank transfer
            // ═══════════════════════════════════════════════════════════════════
            case BankTransfer(var routing, var account, var bankName, var amount, var customerId)
                    when amount.compareTo(VERY_HIGH_VALUE) > 0 -> {

                logger.info("✓ PATTERN MATCHED: BankTransfer with very-high-value guard");
                logger.info("✓ DESTRUCTURED: bank={}, amount=${}", bankName, amount);
                logger.info("✓ GUARD CONDITION: amount > 5000");

                // Track execution steps
                tracker.recordTypeCheck("BankTransfer");
                tracker.recordDestructuringBankTransfer(bankName, amount);
                tracker.recordGuardEvaluationAmountOnly(1, amount, VERY_HIGH_VALUE);

                String message = isInternational
                        ? "Very high-value international transfer (5-7 business days)"
                        : "Very high-value transfer (3-5 business days)";

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.PENDING,
                        amount,
                        "Bank Transfer",
                        message
                );
                resp.setRequiresAdditionalVerification(true);
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }

            // ═══════════════════════════════════════════════════════════════════
            // CASE 7: Standard bank transfer (no guards)
            // ═══════════════════════════════════════════════════════════════════
            case BankTransfer(var routing, var account, var bankName, var amount, var customerId) -> {
                logger.info("✓ PATTERN MATCHED: BankTransfer - standard processing");
                logger.info("✓ DESTRUCTURED: bank={}, amount=${}", bankName, amount);

                // Track execution
                if (!tracker.hasSteps()) {
                    tracker.recordTypeCheck("BankTransfer");
                    tracker.recordDestructuringBankTransfer(bankName, amount);
                }
                // If we reached here from Case 6, amount guard already tracked

                String message = isInternational
                        ? "International bank transfer (2-3 business days)"
                        : "Bank transfer initiated (1-2 business days)";

                PaymentResponse resp = createResponse(
                        generateTransactionId(),
                        PaymentResponse.PaymentStatus.PENDING,
                        amount,
                        "Bank Transfer",
                        message
                );
                resp.setPatternMatchingSteps(tracker.getSteps());
                yield resp;
            }
        };

        logger.info("✓ PATTERN MATCHING COMPLETE");
        logger.info("✓ Transaction ID: {}", response.getTransactionId());
        logger.info("✓ Status: {}", response.getStatus());
        logger.info("✓ Validation: {}", response.getValidationMessage());
        logger.info("✓ Tracked {} execution steps", response.getPatternMatchingSteps().size());
        logger.info("================================");

        return response;
    }

    // Private helper methods - unchanged

    private PaymentResponse createResponse(String transactionId, PaymentResponse.PaymentStatus status,
                                           BigDecimal amount, String paymentMethod, String validationMessage) {
        PaymentResponse response = new PaymentResponse(transactionId, status, amount, paymentMethod);
        response.setValidationMessage(validationMessage);
        return response;
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@***.com";
        String[] parts = email.split("@");
        return "***@" + parts[1];
    }
}
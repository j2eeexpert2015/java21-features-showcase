package org.example.service;


import org.example.dto.payment.PatternMatchingStep;
import org.example.dto.payment.PatternMatchingStep.GuardCondition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to track Java 21 pattern matching execution steps.
 * Keeps the PaymentService clean by encapsulating all tracking logic.
 *
 * This class records:
 * - Type checks (which payment type matched)
 * - Record destructuring (which fields were extracted)
 * - Guard evaluations (which conditions passed/failed)
 */
public class PatternMatchingTracker {

    private final List<PatternMatchingStep> steps = new ArrayList<>();
    private int stepCounter = 1;

    /**
     * Record a type check step
     * Called when pattern matching identifies the payment type
     */
    public void recordTypeCheck(String paymentType) {
        steps.add(new PatternMatchingStep(
                stepCounter++,
                "TYPE_CHECK",
                true,
                "Payment is " + paymentType
        ));
    }

    /**
     * Record destructuring for CreditCard
     * Shows which fields were extracted from the record
     */
    public void recordDestructuringCreditCard(String type, BigDecimal amount, String expiry) {
        String fields = String.format("type=%s, amount=$%s, expiry=%s", type, amount, expiry);
        steps.add(new PatternMatchingStep(
                stepCounter++,
                "DESTRUCTURING",
                true,
                "Extracted: " + fields
        ));
    }

    /**
     * Record destructuring for PayPal
     */
    public void recordDestructuringPayPal(String email, BigDecimal amount) {
        String maskedEmail = maskEmail(email);
        String fields = String.format("email=%s, amount=$%s", maskedEmail, amount);
        steps.add(new PatternMatchingStep(
                stepCounter++,
                "DESTRUCTURING",
                true,
                "Extracted: " + fields
        ));
    }

    /**
     * Record destructuring for BankTransfer
     */
    public void recordDestructuringBankTransfer(String bankName, BigDecimal amount) {
        String fields = String.format("bank=%s, amount=$%s", bankName, amount);
        steps.add(new PatternMatchingStep(
                stepCounter++,
                "DESTRUCTURING",
                true,
                "Extracted: " + fields
        ));
    }

    /**
     * Record guard evaluation with TWO conditions (amount + international)
     * Used for: when amount > 1000 && international
     */
    public void recordGuardEvaluation(int caseNumber, BigDecimal amount, BigDecimal threshold,
                                      boolean isInternational, boolean guardPassed) {
        List<GuardCondition> conditions = new ArrayList<>();

        // First condition: amount check
        boolean amountCheck = amount.compareTo(threshold) > 0;
        conditions.add(new GuardCondition(
                "amount > " + threshold,
                amountCheck,
                String.format("$%s %s $%s", amount, amountCheck ? ">" : "≤", threshold)
        ));

        // Second condition: international check
        conditions.add(new GuardCondition(
                "international",
                isInternational,
                isInternational ? "TRUE" : "FALSE"
        ));

        String expression = String.format("amount > %s && international", threshold);
        String message = guardPassed
                ? "Guard PASSED → Executing this case"
                : "Guard FAILED → Moving to next case";

        steps.add(new PatternMatchingStep(
                stepCounter++,
                "GUARD_EVALUATION",
                guardPassed,
                message,
                caseNumber,
                expression,
                conditions
        ));
    }

    /**
     * Record guard evaluation with ONE condition (amount only)
     * Used for: when amount > threshold
     */
    public void recordGuardEvaluationAmountOnly(int caseNumber, BigDecimal amount, BigDecimal threshold) {
        List<GuardCondition> conditions = new ArrayList<>();

        boolean amountCheck = amount.compareTo(threshold) > 0;
        conditions.add(new GuardCondition(
                "amount > " + threshold,
                amountCheck,
                String.format("$%s %s $%s", amount, amountCheck ? ">" : "≤", threshold)
        ));

        String expression = String.format("amount > %s", threshold);
        String message = amountCheck
                ? "Guard PASSED → Executing this case"
                : "Guard FAILED → Moving to next case";

        steps.add(new PatternMatchingStep(
                stepCounter++,
                "GUARD_EVALUATION",
                amountCheck,
                message,
                caseNumber,
                expression,
                conditions
        ));
    }

    /**
     * Record guard evaluation with ONE condition (international only)
     * Used for: when international
     */
    public void recordGuardEvaluationInternationalOnly(int caseNumber, boolean isInternational) {
        List<GuardCondition> conditions = new ArrayList<>();

        conditions.add(new GuardCondition(
                "international",
                isInternational,
                isInternational ? "TRUE" : "FALSE"
        ));

        String expression = "international";
        String message = isInternational
                ? "Guard PASSED → Executing this case"
                : "Guard FAILED → Moving to next case";

        steps.add(new PatternMatchingStep(
                stepCounter++,
                "GUARD_EVALUATION",
                isInternational,
                message,
                caseNumber,
                expression,
                conditions
        ));
    }

    /**
     * Get all recorded steps (returns a copy to prevent external modification)
     */
    public List<PatternMatchingStep> getSteps() {
        return new ArrayList<>(steps);
    }

    /**
     * Check if any steps have been recorded
     */
    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    /**
     * Mask email for privacy in logs
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.com";
        }
        String[] parts = email.split("@");
        return "***@" + parts[1];
    }
}

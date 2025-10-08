package org.example.concepts.patternmatchingforswitch;

import org.example.model.payment.*;
import java.math.BigDecimal;

/**
 * Pattern Matching for Switch - Step by Step Examples
 * Demonstrates the evolution from traditional Java to Java 21 pattern matching
 */
public class PatternMatchingExamples {

    // ========================================================================
    // FEATURE 1: SWITCH EXPRESSIONS (Java 14)
    // ========================================================================

    /**
     * OLD WAY: Traditional Switch Statement
     * Must declare variable outside, need break statements
     */
    public int getProcessingDaysOldWay(CustomerType customerType) {
        int processingDays;

        switch (customerType) {
            case BASIC:
                processingDays = 5;
                break;
            case PREMIUM:
                processingDays = 2;
                break;
            case VIP:
                processingDays = 1;
                break;
            default:
                processingDays = 7;
                break;
        }

        return processingDays;
    }

    /**
     * NEW WAY: Switch Expression (Java 14)
     * Returns value directly, no break statements, arrow syntax
     */
    public int getProcessingDaysNewWay(CustomerType customerType) {
        return switch (customerType) {
            case BASIC -> 5;
            case PREMIUM -> 2;
            case VIP -> 1; 
        };
    }

    /**
     * Helper method to format processing days as readable label
     */
    public String getProcessingLabel(CustomerType customerType) {
        int days = getProcessingDaysNewWay(customerType);
        return switch (days) {
            case 0 -> "Express processing - Same day";
            case 2 -> "High priority - 1-2 business days";
            case 5 -> "Standard processing - 3-5 business days";
            default -> "Standard processing - " + days + " business days";
        };
    }

    // ========================================================================
    // FEATURE 2: TYPE PATTERNS (Java 16)
    // ========================================================================

    /**
     * OLD WAY: Manual instanceof + Cast
     * Three separate steps: check type, cast, use
     */
    public String processPaymentOldWay(Payment payment) {
        String result;

        if (payment instanceof CreditCard) {
            CreditCard creditCard = (CreditCard) payment;
            result = "Credit Card [" + creditCard.cardType() + "] - $" + creditCard.amount();
        }
        else if (payment instanceof PayPal) {
            PayPal paypal = (PayPal) payment;
            result = "PayPal [" + paypal.email() + "] - $" + paypal.amount();
        }
        else if (payment instanceof BankTransfer) {
            BankTransfer bank = (BankTransfer) payment;
            result = "Bank Transfer [" + bank.bankName() + "] - $" + bank.amount();
        }
        else {
            result = "Unknown payment type";
        }

        return result;
    }

    /**
     * NEW WAY: Type Pattern (Java 16)
     * instanceof checks type, casts, and creates variable in ONE step
     */
    public String processPaymentWithTypePattern(Payment payment) {
        String result;

        if (payment instanceof CreditCard cc) {
            result = "Credit Card [" + cc.cardType() + "] - $" + cc.amount();
        }
        else if (payment instanceof PayPal pp) {
            result = "PayPal [" + pp.email() + "] - $" + pp.amount();
        }
        else if (payment instanceof BankTransfer bt) {
            result = "Bank Transfer [" + bt.bankName() + "] - $" + bt.amount();
        }
        else {
            result = "Unknown payment type";
        }

        return result;
    }

    // ========================================================================
    // FEATURE 3: RECORD PATTERNS (Java 21)
    // ========================================================================

    /**
     * OLD WAY: Type Pattern + Manual Field Access
     * Must call accessor methods for each field
     */
    public String getCreditCardDetailsOldWay(Payment payment) {
        if (payment instanceof CreditCard cc) {
            String type = cc.cardType();
            BigDecimal amount = cc.amount();
            String expiry = cc.expiryDate();

            return "Card: " + type + ", Amount: $" + amount + ", Expires: " + expiry;
        }

        return "Not a credit card";
    }

    /**
     * NEW WAY: Record Pattern with Destructuring (Java 21)
     * Extracts all fields in ONE step, no accessor calls needed
     */
    public String getCreditCardDetailsNewWay(Payment payment) {
        if (payment instanceof CreditCard(var number, var type, var cvv,
                                          var expiry, var amount, var id)) {
            return "Card: " + type + ", Amount: $" + amount + ", Expires: " + expiry;
        }

        return "Not a credit card";
    }

    /**
     * Record Pattern also works with other payment types
     */
    public String getPayPalDetailsWithRecordPattern(Payment payment) {
        if (payment instanceof PayPal(var email, var accountId, var amount, var id)) {
            return "PayPal: " + email + ", Amount: $" + amount + ", Account: " + accountId;
        }

        return "Not a PayPal payment";
    }

    // ========================================================================
    // COMBINING ALL THREE: PATTERN MATCHING FOR SWITCH (Java 21)
    // ========================================================================

    /**
     * ULTIMATE OLD WAY: Before Java 21
     * Manual type checking, casting, field extraction (~20 lines)
     */
    public String processPaymentCompleteOldWay(Payment payment, boolean isInternational) {
        String result;

        if (payment instanceof CreditCard) {
            CreditCard creditCard = (CreditCard) payment;
            String type = creditCard.cardType();
            BigDecimal amount = creditCard.amount();

            if (amount.compareTo(new BigDecimal("1000")) > 0 && isInternational) {
                result = "High-value international Credit Card [" + type + "] - $" + amount + " - Requires verification";
            } else if (amount.compareTo(new BigDecimal("1000")) > 0) {
                result = "High-value Credit Card [" + type + "] - $" + amount;
            } else {
                result = "Credit Card [" + type + "] - $" + amount;
            }
        }
        else if (payment instanceof PayPal) {
            PayPal paypal = (PayPal) payment;
            String email = paypal.email();
            BigDecimal amount = paypal.amount();

            if (isInternational) {
                result = "International PayPal [" + email + "] - $" + amount;
            } else {
                result = "PayPal [" + email + "] - $" + amount;
            }
        }
        else if (payment instanceof BankTransfer) {
            BankTransfer bank = (BankTransfer) payment;
            String bankName = bank.bankName();
            BigDecimal amount = bank.amount();

            result = "Bank Transfer [" + bankName + "] - $" + amount;
        }
        else {
            result = "Unknown payment type";
        }

        return result;
    }

    /**
     * ULTIMATE NEW WAY: Pattern Matching for Switch (Java 21)
     * Combines: Switch Expression + Type Pattern + Record Pattern + Guard Conditions
     * (~10 lines - 50% less code, type-safe, compiler-verified)
     */
    public String processPaymentCompleteNewWay(Payment payment, boolean isInternational) {
        return switch (payment) {

            // High-value international credit card
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 && isInternational ->
                    "High-value international Credit Card [" + type + "] - $" + amount + " - Requires verification";

            // High-value domestic credit card
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 ->
                    "High-value Credit Card [" + type + "] - $" + amount;

            // Standard credit card
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id) ->
                    "Credit Card [" + type + "] - $" + amount;

            // International PayPal
            case PayPal(var email, var accountId, var amount, var id) when isInternational ->
                    "International PayPal [" + email + "] - $" + amount;

            // Standard PayPal
            case PayPal(var email, var accountId, var amount, var id) ->
                    "PayPal [" + email + "] - $" + amount;

            // Bank transfer
            case BankTransfer(var routing, var account, var bankName, var amount, var id) ->
                    "Bank Transfer [" + bankName + "] - $" + amount;
        };
    }

    // ========================================================================
    // DEMO
    // ========================================================================

    public static void main(String[] args) {
        PatternMatchingExamples demo = new PatternMatchingExamples();

        Payment creditCard = new CreditCard(
                "4532-1234-5678-9010", "Visa", "123", "12/25",
                new BigDecimal("1500"), 1L
        );

        Payment paypal = new PayPal(
                "user@example.com", "PP-12345",
                new BigDecimal("500"), 2L
        );

        Payment bank = new BankTransfer(
                "123456789", "987654321", "Chase Bank",
                new BigDecimal("5000"), 3L
        );

        System.out.println("=== FEATURE 1: SWITCH EXPRESSIONS ===");
        System.out.println("OLD: " + demo.getProcessingDaysOldWay(CustomerType.VIP));
        System.out.println("NEW: " + demo.getProcessingDaysNewWay(CustomerType.VIP));
        System.out.println();

        System.out.println("=== FEATURE 2: TYPE PATTERNS ===");
        System.out.println("OLD: " + demo.processPaymentOldWay(creditCard));
        System.out.println("NEW: " + demo.processPaymentWithTypePattern(creditCard));
        System.out.println();

        System.out.println("=== FEATURE 3: RECORD PATTERNS ===");
        System.out.println("OLD: " + demo.getCreditCardDetailsOldWay(creditCard));
        System.out.println("NEW: " + demo.getCreditCardDetailsNewWay(creditCard));
        System.out.println();

        System.out.println("=== ALL COMBINED: PATTERN MATCHING FOR SWITCH ===");
        System.out.println("OLD WAY (~20 lines):");
        System.out.println(demo.processPaymentCompleteOldWay(creditCard, true));
        System.out.println();
        System.out.println("NEW WAY (~10 lines):");
        System.out.println(demo.processPaymentCompleteNewWay(creditCard, true));
        System.out.println();

        System.out.println("=== MORE EXAMPLES ===");
        System.out.println("PayPal: " + demo.processPaymentCompleteNewWay(paypal, false));
        System.out.println("Bank: " + demo.processPaymentCompleteNewWay(bank, false));
    }
}
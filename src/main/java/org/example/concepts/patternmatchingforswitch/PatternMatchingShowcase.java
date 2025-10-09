package org.example.concepts.patternmatchingforswitch;

import org.example.model.payment.*;
import java.math.BigDecimal;

/**
 * Pattern Matching for Switch - Step by Step Examples
 * Demonstrates the evolution from traditional Java to Java 21 pattern matching
 *
 * Learning Path:
 * 1. Java 14: Switch Expressions (returns values)
 * 2. Java 16: Type Patterns (instanceof + cast + bind in one step)
 * 3. Java 21: Pattern Matching for Switch (combines all features + guards)
 */
public class PatternMatchingShowcase {

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
     *
     * Key improvements:
     * - No variable declaration outside switch
     * - Arrow syntax (->) replaces colon (:)
     * - No break statements needed
     * - More concise and readable
     */
    public int getProcessingDaysNewWay(CustomerType customerType) {
        return switch (customerType) {
            case BASIC -> 5;
            case PREMIUM -> 2;
            case VIP -> 1;
            default -> 7;
        };
    }


    /**
     * Helper method to format processing days as readable label
     * Demonstrates: Switch expression with multiple cases AND using enum properties
     */
    public String getProcessingLabel(CustomerType customerType) {
        int days = getProcessingDaysNewWay(customerType);
        String priority = customerType.getPriority(); // Uses the enum's priority field

        return switch (days) {
            case 1 -> priority + " processing - 1 business day";
            case 2 -> priority + " processing - 2 business days";
            case 5 -> priority + " processing - 5 business days";
            default -> "Standard processing - " + days + " business days";
        };
    }

    // ========================================================================
    // FEATURE 2: TYPE PATTERNS (Java 16)
    // ========================================================================

    /**
     * OLD WAY: Manual instanceof + Cast
     * Three separate steps: check type, cast, use
     *
     * Problems with this approach:
     * - Verbose: need explicit cast on separate line
     * - Risk of ClassCastException if cast is wrong
     * - Repetitive code pattern
     */
    public String processPaymentOldWay(Payment payment) {
        String result;

        if (payment instanceof CreditCard) {
            CreditCard creditCard = (CreditCard) payment;  // Manual cast required
            result = "Credit Card [" + creditCard.cardType() + "] - $" + creditCard.amount();
        }
        else if (payment instanceof PayPal) {
            PayPal paypal = (PayPal) payment;  // Manual cast required
            result = "PayPal [" + paypal.email() + "] - $" + paypal.amount();
        }
        else if (payment instanceof BankTransfer) {
            BankTransfer bank = (BankTransfer) payment;  // Manual cast required
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
     *
     * Pattern: if (object instanceof Type variableName)
     * - Checks if object is of Type
     * - Casts it to Type
     * - Binds it to variableName
     * All in ONE expression!
     */
    public String processPaymentWithTypePattern(Payment payment) {
        String result;

        if (payment instanceof CreditCard cc) {  // Type pattern: check + cast + bind
            result = "Credit Card [" + cc.cardType() + "] - $" + cc.amount();
        }
        else if (payment instanceof PayPal pp) {  // No manual cast needed!
            result = "PayPal [" + pp.email() + "] - $" + pp.amount();
        }
        else if (payment instanceof BankTransfer bt) {  // Type-safe and concise
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
     *
     * With records, we still need to call accessor methods like:
     * cc.cardType(), cc.amount(), cc.expiryDate()
     */
    public String getCreditCardDetailsOldWay(Payment payment) {
        if (payment instanceof CreditCard cc) {
            String type = cc.cardType();      // Call accessor
            BigDecimal amount = cc.amount();  // Call accessor
            String expiry = cc.expiryDate();  // Call accessor

            return "Card: " + type + ", Amount: $" + amount + ", Expires: " + expiry;
        }

        return "Not a credit card";
    }

    /**
     * NEW WAY: Record Pattern with Destructuring (Java 21)
     * Extracts all fields in ONE step, no accessor calls needed
     *
     * Pattern: instanceof RecordType(var field1, var field2, ...)
     * - Checks type
     * - Extracts ALL record components
     * - Creates variables for each component
     *
     * Note: Field order MUST match the record definition!
     * CreditCard(cardNumber, cardType, cvv, expiryDate, amount, customerId)
     */
    public String getCreditCardDetailsNewWay(Payment payment) {
        // Record pattern destructures all 6 fields at once!
        if (payment instanceof CreditCard(var number, var type, var cvv,
                                          var expiry, var amount, var id)) {
            // All fields are now available as variables - no accessor calls needed
            return "Card: " + type + ", Amount: $" + amount + ", Expires: " + expiry;
        }

        return "Not a credit card";
    }

    /**
     * Record Pattern also works with other payment types
     * Demonstrates: Destructuring works for any record
     */
    public String getPayPalDetailsWithRecordPattern(Payment payment) {
        // PayPal has 4 components: (email, accountId, amount, customerId)
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
     *
     * This is what we had to write before pattern matching for switch.
     * Notice:
     * - Nested if-else chains
     * - Manual instanceof checks
     * - Manual casting
     * - Manual field access via accessors
     * - Nested if statements for business logic
     *
     * Result: Verbose, hard to read, error-prone
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
     *
     * This ONE switch statement combines FOUR powerful features:
     * 1️⃣ Switch Expression (Java 14)  - returns value directly, no break statements
     * 2️⃣ Type Pattern (Java 16)       - checks if Payment is CreditCard/PayPal/BankTransfer
     * 3️⃣ Record Pattern (Java 21)     - extracts all fields (destructuring) in one step
     * 4️⃣ Guard Condition (Java 21)    - 'when' clause adds business logic filtering
     *
     * Compare: ~40 lines (old way) vs ~15 lines (new way) for the same logic!
     *
     * ⚠️ Pattern Order Matters! Most specific cases (with guards) must come FIRST.
     */
    public String processPaymentCompleteNewWay(Payment payment, boolean isInternational) {
        return switch (payment) {

            // ═══════════════════════════════════════════════════════════════════
            // CREDITCARD PATTERNS - Ordered from most specific to least specific
            // ═══════════════════════════════════════════════════════════════════

            // Pattern breakdown for this case:
            // • CreditCard(...)               → Type check: Is this a CreditCard?
            // • (var number, var type, ...)   → Record destructuring: extract all 6 fields
            // • when amount > 1000 && isInternational → Guard: additional business rule
            // If ALL three conditions are true, execute this case

            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 && isInternational ->
                    "High-value international Credit Card [" + type + "] - $" + amount + " - Requires verification";

            // This catches CreditCards with amount > 1000 that are NOT international
            // Note: Previous guard failed (not international), so we check without it
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 ->
                    "High-value Credit Card [" + type + "] - $" + amount;

            // This catches all remaining CreditCards (amount <= 1000)
            // No guard condition = always matches if type is CreditCard
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id) ->
                    "Credit Card [" + type + "] - $" + amount;

            // ═══════════════════════════════════════════════════════════════════
            // PAYPAL PATTERNS
            // ═══════════════════════════════════════════════════════════════════

            // International PayPal transactions
            case PayPal(var email, var accountId, var amount, var id) when isInternational ->
                    "International PayPal [" + email + "] - $" + amount;

            // Standard PayPal (domestic)
            case PayPal(var email, var accountId, var amount, var id) ->
                    "PayPal [" + email + "] - $" + amount;

            // ═══════════════════════════════════════════════════════════════════
            // BANKTRANSFER PATTERN
            // ═══════════════════════════════════════════════════════════════════

            // BankTransfer has no special rules, just one case
            case BankTransfer(var routing, var account, var bankName, var amount, var id) ->
                    "Bank Transfer [" + bankName + "] - $" + amount;

            // ═══════════════════════════════════════════════════════════════════
            // Note: No default case needed!
            // Payment is a SEALED interface, so compiler knows CreditCard, PayPal,
            // and BankTransfer are the ONLY possible implementations.
            // This is called "exhaustiveness checking" - compiler guarantees we
            // handled all cases. If we add a new Payment type, compiler will
            // force us to handle it here. Type-safe! ✅
            // ═══════════════════════════════════════════════════════════════════
        };
    }

    // ========================================================================
    // DEMO
    // ========================================================================

    public static void main(String[] args) {
        PatternMatchingShowcase demo = new PatternMatchingShowcase();

        // Create test payment objects for various scenarios
        Payment highValueCredit = new CreditCard(
                "4532-1234-5678-9010", "Visa", "123", "12/25",
                new BigDecimal("1500"), 1L
        );

        Payment lowValueCredit = new CreditCard(
                "4532-9999-8888-7777", "Mastercard", "456", "06/26",
                new BigDecimal("50"), 2L
        );

        Payment paypal = new PayPal(
                "user@example.com", "PP-12345",
                new BigDecimal("500"), 3L
        );

        Payment bank = new BankTransfer(
                "123456789", "987654321", "Chase Bank",
                new BigDecimal("5000"), 4L
        );

        // ═══════════════════════════════════════════════════════════════════════════
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║     JAVA 21 PATTERN MATCHING - COMPLETE DEMONSTRATION        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════════
        // FEATURE 1: SWITCH EXPRESSIONS (Java 14)
        // ═══════════════════════════════════════════════════════════════════════════
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  1️⃣  FEATURE 1: SWITCH EXPRESSIONS (Java 14)                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📊 Processing Days for All Customer Types:");
        System.out.println();

        for (CustomerType type : CustomerType.values()) {
            int oldWay = demo.getProcessingDaysOldWay(type);
            int newWay = demo.getProcessingDaysNewWay(type);
            String label = demo.getProcessingLabel(type);

            System.out.println("🔹 " + type.getDisplayName() + " Customer:");
            System.out.println("   OLD way: " + oldWay + " days");
            System.out.println("   NEW way: " + newWay + " days");
            System.out.println("   Label: " + label);
            System.out.println();
        }

        System.out.println("✅ Benefit: Switch expressions return values directly, no break needed!");
        System.out.println();
        System.out.println("─".repeat(67));
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════════
        // FEATURE 2: TYPE PATTERNS (Java 16)
        // ═══════════════════════════════════════════════════════════════════════════
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  2️⃣  FEATURE 2: TYPE PATTERNS (Java 16)                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("🔍 Processing Payments - Type Pattern vs Manual Cast:");
        System.out.println();

        System.out.println("🔹 Credit Card Payment:");
        System.out.println("   OLD way: " + demo.processPaymentOldWay(highValueCredit));
        System.out.println("   NEW way: " + demo.processPaymentWithTypePattern(highValueCredit));
        System.out.println();

        System.out.println("🔹 PayPal Payment:");
        System.out.println("   OLD way: " + demo.processPaymentOldWay(paypal));
        System.out.println("   NEW way: " + demo.processPaymentWithTypePattern(paypal));
        System.out.println();

        System.out.println("🔹 Bank Transfer:");
        System.out.println("   OLD way: " + demo.processPaymentOldWay(bank));
        System.out.println("   NEW way: " + demo.processPaymentWithTypePattern(bank));
        System.out.println();

        System.out.println("✅ Benefit: instanceof + cast + variable binding in ONE step!");
        System.out.println();
        System.out.println("─".repeat(67));
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════════
        // FEATURE 3: RECORD PATTERNS (Java 21)
        // ═══════════════════════════════════════════════════════════════════════════
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  3️⃣  FEATURE 3: RECORD PATTERNS (Java 21)                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📦 Destructuring Records - Extracting Fields:");
        System.out.println();

        System.out.println("🔹 Credit Card Details:");
        System.out.println("   OLD way: " + demo.getCreditCardDetailsOldWay(highValueCredit));
        System.out.println("   NEW way: " + demo.getCreditCardDetailsNewWay(highValueCredit));
        System.out.println();

        System.out.println("🔹 PayPal Details:");
        System.out.println("   Record Pattern: " + demo.getPayPalDetailsWithRecordPattern(paypal));
        System.out.println();

        System.out.println("✅ Benefit: Extract ALL record fields in ONE step, no accessor calls!");
        System.out.println();
        System.out.println("─".repeat(67));
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════════
        // FEATURE 4: ALL COMBINED - PATTERN MATCHING FOR SWITCH (Java 21)
        // ═══════════════════════════════════════════════════════════════════════════
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║  4️⃣  ALL FEATURES COMBINED: PATTERN MATCHING FOR SWITCH      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("🎯 Testing ALL Payment Scenarios with Guards:");
        System.out.println();

        // Credit Card Scenarios
        System.out.println("💳 CREDIT CARD SCENARIOS:");
        System.out.println();

        System.out.println("  Scenario 1: High-Value ($1500) + International");
        System.out.println("  OLD: " + demo.processPaymentCompleteOldWay(highValueCredit, true));
        System.out.println("  NEW: " + demo.processPaymentCompleteNewWay(highValueCredit, true));
        System.out.println();

        System.out.println("  Scenario 2: High-Value ($1500) + Domestic");
        System.out.println("  OLD: " + demo.processPaymentCompleteOldWay(highValueCredit, false));
        System.out.println("  NEW: " + demo.processPaymentCompleteNewWay(highValueCredit, false));
        System.out.println();

        System.out.println("  Scenario 3: Low-Value ($50) + Domestic");
        System.out.println("  OLD: " + demo.processPaymentCompleteOldWay(lowValueCredit, false));
        System.out.println("  NEW: " + demo.processPaymentCompleteNewWay(lowValueCredit, false));
        System.out.println();

        // PayPal Scenarios
        System.out.println("💰 PAYPAL SCENARIOS:");
        System.out.println();

        System.out.println("  Scenario 4: PayPal ($500) + International");
        System.out.println("  OLD: " + demo.processPaymentCompleteOldWay(paypal, true));
        System.out.println("  NEW: " + demo.processPaymentCompleteNewWay(paypal, true));
        System.out.println();

        System.out.println("  Scenario 5: PayPal ($500) + Domestic");
        System.out.println("  OLD: " + demo.processPaymentCompleteOldWay(paypal, false));
        System.out.println("  NEW: " + demo.processPaymentCompleteNewWay(paypal, false));
        System.out.println();

        // Bank Transfer Scenario
        System.out.println("🏦 BANK TRANSFER SCENARIO:");
        System.out.println();

        System.out.println("  Scenario 6: Bank Transfer ($5000)");
        System.out.println("  OLD: " + demo.processPaymentCompleteOldWay(bank, false));
        System.out.println("  NEW: " + demo.processPaymentCompleteNewWay(bank, false));
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════════
        // SUMMARY
        // ═══════════════════════════════════════════════════════════════════════════
        System.out.println("═".repeat(67));
        System.out.println();
        System.out.println("📈 SUMMARY: Pattern Matching for Switch Benefits");
        System.out.println();
        System.out.println("✅ Combines 4 powerful features in ONE switch statement:");
        System.out.println("   1️⃣  Switch expressions (returns values)");
        System.out.println("   2️⃣  Type patterns (instanceof + cast + bind)");
        System.out.println("   3️⃣  Record patterns (destructuring)");
        System.out.println("   4️⃣  Guard conditions (business logic with 'when')");
        System.out.println();
        System.out.println("✅ Code Reduction: ~40 lines (old) → ~15 lines (new) for same logic");
        System.out.println("✅ More Readable: Clear pattern matching syntax");
        System.out.println("✅ Type-Safe: Compiler-checked exhaustiveness (sealed interfaces)");
        System.out.println("✅ Maintainable: Easy to add new payment types or rules");
        System.out.println();
        System.out.println("═".repeat(67));
    }
}
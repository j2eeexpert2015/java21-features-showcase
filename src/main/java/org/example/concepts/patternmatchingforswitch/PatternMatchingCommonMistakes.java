package org.example.concepts.patternmatchingforswitch;

import org.example.model.payment.*;
import java.math.BigDecimal;

/**
 * Common Mistakes in Pattern Matching for Switch
 *
 * This class demonstrates typical errors students make when learning
 * pattern matching, along with the correct approach.
 *
 * Each mistake is presented as:
 * ❌ WRONG - What NOT to do
 * ✅ CORRECT - The right approach
 */
public class PatternMatchingCommonMistakes {

    // ========================================================================
    // MISTAKE 1: WRONG ORDER - CATCH-ALL BEFORE SPECIFIC CASES
    // ========================================================================

    /**
     * ❌ WRONG: Catch-all case comes BEFORE specific cases with guards
     *
     * Problem: The first matching pattern wins! Once a CreditCard matches
     * the catch-all case, the guards below will NEVER be checked.
     *
     * The code below will NOT compile - Java detects the unreachable case.
     *
     * WRONG CODE (commented out - won't compile):
     *
     * return switch (payment) {
     *     case CreditCard cc -> "Any credit card";  // ❌ Catches ALL CreditCards
     *
     *     case CreditCard(var n, var t, var c, var e, var amount, var id)
     *             when amount.compareTo(new BigDecimal("1000")) > 0 ->
     *             "High-value card";  // ⚠️ UNREACHABLE - dominated by case above!
     *
     *     case PayPal pp -> "PayPal";
     *     case BankTransfer bt -> "Bank";
     * };
     *
     * Error: "Label is dominated by a preceding case label"
     */
    public String wrongOrderCatchAllFirst(Payment payment) {
        // This is what students WANT to write but it won't compile
        // Demonstrating with a less obvious mistake that DOES compile but has wrong logic
        return switch (payment) {
            // Without proper ordering, you might write logic that compiles
            // but doesn't work as intended
            case CreditCard cc -> {
                // Putting logic INSIDE the case - verbose and defeats the purpose
                if (cc.amount().compareTo(new BigDecimal("1000")) > 0) {
                    yield "High-value card";
                } else {
                    yield "Standard card";
                }
            }

            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank";
        };
    }

    /**
     * ✅ CORRECT: Most specific cases with guards come FIRST
     *
     * Rule: Order patterns from most specific to least specific
     * - Guards make patterns more specific
     * - Put guarded cases before unguarded cases
     */
    public String correctOrderGuardsFirst(Payment payment) {
        return switch (payment) {
            // ✅ Most specific: CreditCard with guard
            case CreditCard(var n, var t, var c, var e, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 ->
                    "High-value card";

            // ✅ Less specific: CreditCard without guard (catches remaining)
            case CreditCard cc -> "Standard card";

            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank";
        };
    }

    // ========================================================================
    // MISTAKE 2: WRONG FIELD ORDER IN RECORD PATTERNS
    // ========================================================================

    /**
     * ❌ WRONG: Fields in wrong order in record pattern
     *
     * Problem: Record pattern fields MUST match the exact order defined
     * in the record declaration.
     *
     * CreditCard is defined as:
     * record CreditCard(String cardNumber, String cardType, String cvv,
     *                   String expiryDate, BigDecimal amount, Long customerId)
     *
     * This code will NOT compile - compilation error!
     */
    public String wrongFieldOrder(Payment payment) {
        return switch (payment) {
            // ❌ COMPILATION ERROR: cardType and cardNumber are swapped!
            // Trying to assign String to String works, but the ORDER is wrong
            // case CreditCard(var type, var number, var cvv, var expiry, var amount, var id) ->
            //         "Card: " + type;

            // ✅ This compiles because order matches record definition
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id) ->
                    "Card: " + type;

            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank";
        };
    }

    /**
     * ✅ CORRECT: Fields in exact order as record definition
     *
     * Remember: Record pattern order = Record declaration order
     * Always check your record definition first!
     */
    public String correctFieldOrder(Payment payment) {
        return switch (payment) {
            // ✅ Correct order: matches CreditCard(cardNumber, cardType, cvv, expiryDate, amount, customerId)
            case CreditCard(var number, var type, var cvv, var expiry, var amount, var id) ->
                    "Card: " + type + ", Number: " + number;

            case PayPal(var email, var accountId, var amount, var id) ->
                    "PayPal: " + email;

            case BankTransfer(var routing, var account, var bankName, var amount, var id) ->
                    "Bank: " + bankName;
        };
    }

    // ========================================================================
    // MISTAKE 3: FORGETTING DEFAULT CASE WITH NON-SEALED TYPES
    // ========================================================================

    /**
     * ❌ WRONG: Missing default case when working with non-sealed types
     *
     * Problem: If Payment was NOT sealed, this would be a compilation error
     * because the compiler can't guarantee exhaustiveness.
     *
     * Note: Our Payment IS sealed, so this actually works. But with
     * regular interfaces/classes, you'd need a default case.
     */
    // This method demonstrates the concept - commented out to avoid confusion
    /*
    public String missingDefaultCase(Object obj) {
        return switch (obj) {
            case String s -> "String: " + s;
            case Integer i -> "Integer: " + i;
            // ❌ COMPILATION ERROR if obj is not sealed!
            // Missing: default case
        };
    }
    */

    /**
     * ✅ CORRECT: Include default case for non-sealed types
     *
     * When NOT using sealed types, always include a default case
     * to handle unexpected types.
     */
    public String correctWithDefaultCase(Object obj) {
        return switch (obj) {
            case String s -> "String: " + s;
            case Integer i -> "Integer: " + i;
            case Double d -> "Double: " + d;
            default -> "Unknown type: " + obj.getClass().getSimpleName();
        };
    }

    /**
     * ✅ BONUS: With sealed types, no default needed
     *
     * Sealed interfaces provide exhaustiveness checking.
     * Compiler knows ALL possible subtypes, so no default needed.
     */
    public String sealedTypesNoDefault(Payment payment) {
        return switch (payment) {
            // Sealed interface guarantees only these 3 implementations exist
            case CreditCard cc -> "Credit Card";
            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank Transfer";
            // ✅ No default needed - compiler knows these are ALL possibilities
        };
    }

    // ========================================================================
    // MISTAKE 4: NOT UNDERSTANDING GUARD EVALUATION
    // ========================================================================

    /**
     * ❌ WRONG: Assuming guards work like if-else inside cases
     *
     * Problem: When a guard fails (returns false), the switch moves to
     * the NEXT case, not to nested logic within the same case.
     *
     * Students often think guards work like if-statements INSIDE a case.
     */
    public String wrongGuardUnderstanding(Payment payment, boolean isInternational) {
        return switch (payment) {
            // ❌ Common misconception: Students think if this guard fails,
            // it will "fall through" to some nested else logic
            case CreditCard(var n, var t, var c, var e, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 && isInternational ->
                    "High-value international card";

            // ⚠️ Reality: When guard above fails, execution moves HERE
            // to check if this pattern matches
            case CreditCard cc -> "Any other card";

            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank";
        };
    }

    /**
     * ✅ CORRECT: Understanding guard evaluation order
     *
     * How guards work:
     * 1️⃣ Check if pattern matches (type check)
     * 2️⃣ If pattern matches, evaluate the guard (when clause)
     * 3️⃣ If guard is true, execute this case
     * 4️⃣ If guard is false, move to NEXT case and repeat
     */
    public String correctGuardUnderstanding(Payment payment, boolean isInternational) {
        return switch (payment) {
            // Step 1: Is it a CreditCard? Yes → Continue
            // Step 2: Is amount > 1000 AND isInternational?
            //         - If YES → Execute this case
            //         - If NO → Try next case
            case CreditCard(var n, var t, var c, var e, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 && isInternational ->
                    "High-value international card";

            // Step 1: Is it a CreditCard? Yes → Continue
            // Step 2: Is amount > 1000?
            //         - If YES → Execute this case
            //         - If NO → Try next case
            case CreditCard(var n, var t, var c, var e, var amount, var id)
                    when amount.compareTo(new BigDecimal("1000")) > 0 ->
                    "High-value domestic card";

            // Step 1: Is it a CreditCard? Yes → Execute (no guard to check)
            case CreditCard cc -> "Standard card";

            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank";
        };
    }

    // ========================================================================
    // MISTAKE 5: USING VAR INCORRECTLY
    // ========================================================================

    /**
     * ❌ WRONG: Trying to use 'var' for unused fields
     *
     * Problem: Every field in the record pattern must be declared,
     * even if you don't use it. You can't skip fields.
     */
    public String wrongVarUsage(Payment payment) {
        return switch (payment) {
            // ❌ This won't compile - can't skip fields!
            // case CreditCard(var number, var type) -> "Card";  // Missing 4 fields!

            // ✅ Must declare all fields, use _ for unused ones (Java 21+)
            case CreditCard(var number, var type, var _, var _, var amount, var _) ->
                    "Card: " + type + ", Amount: $" + amount;

            case PayPal pp -> "PayPal";
            case BankTransfer bt -> "Bank";
        };
    }

    /**
     * ✅ CORRECT: Use underscore (_) for unused fields
     *
     * In Java 21+, you can use _ (underscore) as an unnamed variable
     * for fields you don't need to access.
     */
    public String correctUnnamedVariables(Payment payment) {
        return switch (payment) {
            // ✅ Using _ for fields we don't need (cvv, expiry, customerId)
            case CreditCard(var number, var type, var _, var _, var amount, var _) ->
                    "Card: " + type + ", Amount: $" + amount;

            // ✅ Only extracting email and amount, ignore accountId and customerId
            case PayPal(var email, var _, var amount, var _) ->
                    "PayPal: " + email + ", Amount: $" + amount;

            case BankTransfer bt -> "Bank Transfer";
        };
    }

    // ========================================================================
    // DEMO - SHOWING ALL MISTAKES VS CORRECTIONS
    // ========================================================================

    public static void main(String[] args) {
        PatternMatchingCommonMistakes demo = new PatternMatchingCommonMistakes();

        Payment highValueCard = new CreditCard(
                "4532-1234-5678-9010", "Visa", "123", "12/25",
                new BigDecimal("1500"), 1L
        );

        Payment lowValueCard = new CreditCard(
                "4532-9999-8888-7777", "Mastercard", "456", "06/26",
                new BigDecimal("50"), 2L
        );

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        PATTERN MATCHING - COMMON MISTAKES                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // MISTAKE 1: Wrong Order
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("❌ MISTAKE 1: Catch-all Before Specific Cases");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Testing with high-value card ($1500):");
        System.out.println("WRONG order: " + demo.wrongOrderCatchAllFirst(highValueCard));
        System.out.println("  ⚠️  Should say 'High-value card' but says 'Any credit card'");
        System.out.println();
        System.out.println("CORRECT order: " + demo.correctOrderGuardsFirst(highValueCard));
        System.out.println("  ✅ Correctly identifies as 'High-value card'");
        System.out.println();

        // MISTAKE 2: Wrong Field Order
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("❌ MISTAKE 2: Wrong Field Order in Record Pattern");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("CORRECT order: " + demo.correctFieldOrder(highValueCard));
        System.out.println("  ✅ Fields must match record declaration order exactly");
        System.out.println();

        // MISTAKE 3: Default Case
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("❌ MISTAKE 3: Missing Default Case (Non-Sealed Types)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("With Object (non-sealed): " + demo.correctWithDefaultCase("Hello"));
        System.out.println("  ✅ Default case handles unexpected types");
        System.out.println();
        System.out.println("With Payment (sealed): " + demo.sealedTypesNoDefault(highValueCard));
        System.out.println("  ✅ Sealed types don't need default - exhaustiveness guaranteed");
        System.out.println();

        // MISTAKE 4: Guard Evaluation
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("❌ MISTAKE 4: Not Understanding Guard Evaluation");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("High-value domestic card:");
        System.out.println("Result: " + demo.correctGuardUnderstanding(highValueCard, false));
        System.out.println("  ✅ Guard checks happen sequentially, failed guards move to next case");
        System.out.println();

        // MISTAKE 5: Var Usage
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("❌ MISTAKE 5: Using 'var' Incorrectly");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Using _ for unused fields: " + demo.correctUnnamedVariables(highValueCard));
        System.out.println("  ✅ Use underscore (_) for fields you don't need");
        System.out.println();

        System.out.println("═".repeat(67));
        System.out.println("💡 KEY TAKEAWAY: Order matters, fields must match, understand guards!");
        System.out.println("═".repeat(67));
    }
}
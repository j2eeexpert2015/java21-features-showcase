// src/main/java/org/example/dto/payment/PatternMatchingStep.java
package org.example.dto.payment;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single step in the Java 21 pattern matching execution flow.
 * Used to track and visualize how pattern matching works in real-time.
 *
 * Types of steps:
 * - TYPE_CHECK: Verifying the payment type (CreditCard, PayPal, BankTransfer)
 * - DESTRUCTURING: Extracting record fields (type, amount, etc.)
 * - GUARD_EVALUATION: Evaluating guard conditions (when clauses)
 */
public class PatternMatchingStep {

    private int number;              // Step number in sequence (1, 2, 3...)
    private String type;             // TYPE_CHECK | DESTRUCTURING | GUARD_EVALUATION
    private boolean passed;          // Whether this step passed/succeeded
    private String message;          // Human-readable description

    // Only for GUARD_EVALUATION steps:
    private Integer caseNumber;      // Which case in the switch (1, 2, 3...)
    private String guardExpression;  // The guard condition text (e.g., "amount > 1000 && international")
    private List<GuardCondition> conditions; // Individual condition results

    // Constructors

    /**
     * Constructor for simple steps (TYPE_CHECK, DESTRUCTURING)
     */
    public PatternMatchingStep(int number, String type, boolean passed, String message) {
        this.number = number;
        this.type = type;
        this.passed = passed;
        this.message = message;
        this.conditions = new ArrayList<>();
    }

    /**
     * Constructor for GUARD_EVALUATION steps
     */
    public PatternMatchingStep(int number, String type, boolean passed, String message,
                               int caseNumber, String guardExpression, List<GuardCondition> conditions) {
        this.number = number;
        this.type = type;
        this.passed = passed;
        this.message = message;
        this.caseNumber = caseNumber;
        this.guardExpression = guardExpression;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    // Getters and Setters

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(Integer caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getGuardExpression() {
        return guardExpression;
    }

    public void setGuardExpression(String guardExpression) {
        this.guardExpression = guardExpression;
    }

    public List<GuardCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<GuardCondition> conditions) {
        this.conditions = conditions;
    }

    /**
     * Nested class representing a single condition within a guard clause
     * Example: "amount > 1000" is one condition
     */
    public static class GuardCondition {
        private String name;        // Condition name (e.g., "amount > 1000")
        private boolean passed;     // Whether condition evaluated to true
        private String result;      // Human-readable result (e.g., "$1500 > $1000")

        public GuardCondition(String name, boolean passed, String result) {
            this.name = name;
            this.passed = passed;
            this.result = result;
        }

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
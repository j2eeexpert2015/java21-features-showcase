package org.example.concepts.stringtemplate.basic;

public class StringFormatBugSimulation {
    public static void main(String[] args) {
        // ===== BEFORE: Working fine =====
        String customerName = "John Smith";
        String orderId = "ORD-12345";
        double amount = 15.00;

        String email = String.format(
                "Dear %s, Your order %s for $%.2f is confirmed",
                customerName, orderId, amount
        );

        System.out.println("BEFORE (Working):");
        System.out.println(email);
        // Output: Dear John Smith, Your order ORD-12345 for $15.00 is confirmed

        System.out.println("\n" + "=".repeat(60) + "\n");

        // ===== AFTER: Someone adds discount field =====
        double discount = 10.0;  // NEW field added

        // Developer accidentally puts discount first (replacing customerName)!
        String emailAfterDiscountAdded = String.format(
                "Dear %s, Your order %s for $%.2f is confirmed",
                discount, orderId, amount  // WRONG! discount replaced customerName
        );

        System.out.println("AFTER (BROKEN - discount replaced customerName):");
        System.out.println(emailAfterDiscountAdded);
        // Output: Dear 10.0, Your order ORD-12345 for $15.00 is confirmed
        //         ^^^^^ WRONG! Should be "John Smith"
    }
}
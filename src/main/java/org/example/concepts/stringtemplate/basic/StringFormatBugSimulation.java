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

        // Developer updates format string - uses %s for everything to avoid type issues
        // But adds discount argument at the BEGINNING
        // This pushes all existing arguments down by one position!
        String emailAfterDiscountAdded = String.format(
                "Dear %s, Your order %s for $%s with %s%% discount is confirmed",
                discount,      // NEW at position 1 - goes to "Dear %s" → "Dear 10.0"
                customerName,  // Pushed to position 2 - goes to "order %s" → "order John Smith"
                orderId,       // Pushed to position 3 - goes to "$%s" → "$ORD-12345"
                amount         // Pushed to position 4 - goes to "%s%% discount" → "15.0% discount"
        );

        System.out.println("AFTER (BROKEN - arguments shifted):");
        System.out.println(emailAfterDiscountAdded);
        // Output: Dear 10.0, Your order John Smith for $ORD-12345 with 15.0% discount is confirmed
        //         ^^^^^ WRONG! Everything is in the wrong place!

        System.out.println("\n" + "=".repeat(60) + "\n");

        // ===== WHAT IT SHOULD BE: Correct version =====
        String emailCorrect = String.format(
                "Dear %s, Your order %s for $%s with %s%% discount is confirmed",
                customerName,  // position 1 - CORRECT
                orderId,       // position 2 - CORRECT
                amount,        // position 3 - CORRECT
                discount       // position 4 - CORRECT
        );

        System.out.println("CORRECT VERSION:");
        System.out.println(emailCorrect);
        // Output: Dear John Smith, Your order ORD-12345 for $15.0 with 10.0% discount is confirmed
    }
}
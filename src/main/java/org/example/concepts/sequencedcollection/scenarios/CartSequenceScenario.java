package org.example.concepts.sequencedcollection.scenarios;

import java.util.ArrayList;
import java.util.List;

/**
 * This shows how Sequenced Collections make order explicit and operations intuitive—e.g., no need for add(0, item) or remove(size-1).
 * Can be efficient for end-based access in many scenarios for example e-commerce carts.
 */
public class CartSequenceScenario {

    record CartItem(String name, double price) {
        @Override
        public String toString() {
            return name + " ($" + price + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 🛒 Cart Sequence Scenario ===");
        System.out.println("Demonstrating SequencedCollection methods on shopping cart\n");
        // Start with empty cart
        List<CartItem> cart = new ArrayList<>();
        System.out.println("Initial cart: " + cart);  // Empty

        // Normal add (addLast)
        cart.addLast(new CartItem("iPhone", 999.99));
        cart.addLast(new CartItem("MacBook", 1999.99));
        System.out.println("After normal adds: " + cart);

        // Priority add (addFirst)
        cart.addFirst(new CartItem("AirPods", 249.99));
        System.out.println("After priority add: " + cart);

        // Print badges
        if (!cart.isEmpty()) {
            System.out.println("Oldest item: " + cart.getFirst());
            System.out.println("Newest item: " + cart.getLast());
        }

        // Undo last action (removeLast)
        if (!cart.isEmpty()) {
            CartItem removed = cart.removeLast();
            System.out.println("Undid last add: Removed " + removed);
            System.out.println("Cart after undo: " + cart);
        }

        // Edge case: Try undo on potentially empty cart
        if (!cart.isEmpty()) {
            cart.removeLast();
            System.out.println("Another undo: " + cart);
        } else {
            System.out.println("Cannot undo: Cart is empty");
        }
    }
}

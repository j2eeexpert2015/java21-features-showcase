package org.example.concepts.sequencedcollection.scenarios;

import java.util.ArrayList;
import java.util.SequencedCollection;

/**
 * Shows how Sequenced Collections make order explicit and operations intuitive.
 * No need for add(0, item) or remove(size-1) - use addFirst/addLast instead.
 * Efficient for end-based access in e-commerce carts.
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

        /*
         * Using SequencedCollection (ArrayList implementation)
         * for shopping cart operations
         */
        System.out.println("→ Using SequencedCollection for cart");
        SequencedCollection<CartItem> cartList = new ArrayList<>();
        System.out.println("Initial cart: " + cartList);

        /* Normal add (addLast) - items added to end of cart */
        cartList.addLast(new CartItem("iPhone", 999.99));
        cartList.addLast(new CartItem("MacBook", 1999.99));
        System.out.println("After normal adds: " + cartList);

        /* Priority add (addFirst) - urgent item goes to front */
        cartList.addFirst(new CartItem("AirPods", 249.99));
        System.out.println("After priority add: " + cartList);

        /* Access first and last items easily */
        if (!cartList.isEmpty()) {
            System.out.println("Oldest item: " + cartList.getFirst());
            System.out.println("Newest item: " + cartList.getLast());
        }

        /* Undo last action (removeLast) */
        if (!cartList.isEmpty()) {
            CartItem removed = cartList.removeLast();
            System.out.println("Undid last add: Removed " + removed);
            System.out.println("Cart after undo: " + cartList);
        }

        /* Another undo */
        if (!cartList.isEmpty()) {
            CartItem removed = cartList.removeLast();
            System.out.println("Again Removed :" + removed);
            System.out.println("Cart after undo: " + cartList);
        } else {
            System.out.println("Cannot undo: Cart is empty");
        }
    }
}
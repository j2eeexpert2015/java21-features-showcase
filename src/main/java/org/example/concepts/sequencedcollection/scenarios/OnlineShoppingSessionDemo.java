package org.example.concepts.sequencedcollection.scenarios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;

/**
 * A realistic demo showing how different Sequenced Collections manage a user's shopping session.
 * - SequencedMap: Tracks session events in chronological order
 * - SequencedSet: Manages unique 'recently viewed' products in order
 * - SequencedCollection: Represents the shopping cart with ordered items
 */
public class OnlineShoppingSessionDemo {

    record Product(int id, String name) {
        @Override
        public String toString() {
            return name + " (#" + id + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 🛍️ Online Shopping User Session Demo ===");

        /*
         * 1. SESSION DATA (SequencedMap)
         * LinkedHashMap preserves the order in which events occurred
         */
        System.out.println("\n→ Using SequencedMap for session tracking");
        SequencedMap<String, Object> sessionDataMap = new LinkedHashMap<>();
        sessionDataMap.putLast("userId", "user-123");
        sessionDataMap.putLast("loginTime", Instant.now());

        System.out.println("\n--- Session Started ---");
        System.out.println("Session Data: " + sessionDataMap);

        /*
         * 2. RECENTLY VIEWED PRODUCTS (SequencedSet)
         * LinkedHashSet maintains insertion order AND guarantees uniqueness
         * Perfect for "Recently Viewed" feature - no duplicates, but order matters
         */
        System.out.println("\n→ Using SequencedSet for recently viewed products");
        SequencedSet<Product> viewedProductsSet = new LinkedHashSet<>();

        Product laptop = new Product(101, "Laptop");
        Product mouse = new Product(102, "Mouse");
        Product keyboard = new Product(103, "Keyboard");

        System.out.println("\n--- User Browsing Products ---");
        viewedProductsSet.addLast(laptop);
        viewedProductsSet.addLast(mouse);
        System.out.println("After viewing 2 products: " + viewedProductsSet);

        /* User views laptop again - duplicate is ignored due to Set behavior */
        viewedProductsSet.addLast(laptop);
        System.out.println("After viewing Laptop again (no duplicate): " + viewedProductsSet);

        System.out.println("First viewed: " + viewedProductsSet.getFirst());
        System.out.println("Most recent: " + viewedProductsSet.getLast());

        /*
         * 3. SHOPPING CART (SequencedCollection)
         * ArrayList maintains the order items were added
         */
        System.out.println("\n→ Using SequencedCollection for shopping cart");
        SequencedCollection<Product> shoppingCartList = new ArrayList<>();

        System.out.println("\n--- User Adding to Cart ---");
        shoppingCartList.addLast(mouse);
        shoppingCartList.addLast(keyboard);
        System.out.println("Cart: " + shoppingCartList);

        /* "Buy Now" adds item to front for immediate checkout */
        System.out.println("\nUser clicks 'Buy Now' on Laptop (priority item)...");
        shoppingCartList.addFirst(laptop);
        System.out.println("Cart with priority item: " + shoppingCartList);

        /* Show cart in reverse (useful for "last added" view) */
        System.out.println("Cart (newest first): " + shoppingCartList.reversed());

        /* 4. FINAL SESSION STATE */
        sessionDataMap.putLast("finalCart", shoppingCartList);
        sessionDataMap.putLast("viewedProducts", viewedProductsSet.size());
        sessionDataMap.putLast("logoutTime", Instant.now());

        System.out.println("\n--- Session Ended ---");
        System.out.println("First session event: " + sessionDataMap.firstEntry());
        System.out.println("Last session event: " + sessionDataMap.lastEntry());

        System.out.println("\n📊 Session Summary:");
        System.out.println("Total products viewed: " + viewedProductsSet.size());
        System.out.println("Items in cart: " + shoppingCartList.size());
        System.out.println("Priority item: " + shoppingCartList.getFirst());
    }
}
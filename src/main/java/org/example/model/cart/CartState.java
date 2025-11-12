package org.example.model.cart;

import java.util.ArrayList;
import java.util.SequencedCollection;
import java.math.BigDecimal;

/**
 * CartState - The complete state of a customer's shopping cart
 *
 * This class demonstrates Java 21 Sequenced Collections best practices:
 * - Uses SequencedCollection instead of List for explicit ordering
 * - Leverages getFirst() and getLast() for metadata
 * - Maintains both items and action history
 *
 * 🎯 KEY IMPROVEMENT: Using SequencedCollection instead of List
 *
 * Why SequencedCollection?
 * ✅ Makes ordering guarantees EXPLICIT in the API
 * ✅ Provides getFirst(), getLast(), addFirst(), addLast(), removeFirst(), removeLast()
 * ✅ Better semantic meaning - "this collection maintains insertion order"
 * ✅ Future-proof - designed for Java 21+ features
 */
public class CartState {

    // ❌ MISTAKE #1: Using generic List - doesn't communicate ordering guarantees
    // private List<CartItem> items = new ArrayList<>();
    // private List<CartItem> actionHistory = new ArrayList<>();

    // ✅ FIX #1: Use SequencedCollection - explicitly shows we care about order
    // 💡 BEST PRACTICE: Declare with SequencedCollection interface, implement with ArrayList
    private SequencedCollection<CartItem> items = new ArrayList<>();
    private SequencedCollection<CartItem> actionHistory = new ArrayList<>();

    // ✅ FIX #2: Maintain metadata for first/last item tracking using Java 21 APIs
    // These fields are dynamically updated after every cart modification
    private Product oldestItem;
    private Product newestItem;

    public CartState() {}

    /**
     * ✅ BEST PRACTICE: Update metadata using Java 21 Sequenced Collections API
     *
     * 💡 Why this is better than items.get(0) and items.get(items.size() - 1):
     * - More readable: getFirst() vs get(0)
     * - Safer: No IndexOutOfBoundsException risk
     * - Intent-revealing: Code clearly shows "get the first/last element"
     * - Consistent API: Works across all SequencedCollection implementations
     *
     * 🔧 NOTE:
     * Call this method after every add/remove/clear operation to keep UI metadata correct.
     */
    public void updateMetadata() {
        if (items == null || items.isEmpty()) {
            this.oldestItem = null;
            this.newestItem = null;
            return;
        }

        // ✅ Java 21 SequencedCollection APIs
        this.oldestItem = items.getFirst().getProduct(); // getFirst() = oldest
        this.newestItem = items.getLast().getProduct();  // getLast() = newest
    }

    /**
     * ✅ BEST PRACTICE: Calculate total using stream operations
     * Demonstrates combining Java 8 Streams with Java 21 Collections
     */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * @return SequencedCollection guaranteeing insertion order is maintained
     */
    public SequencedCollection<CartItem> getItems() {
        return items;
    }

    /**
     * @param items SequencedCollection ensuring order preservation
     */
    public void setItems(SequencedCollection<CartItem> items) {
        this.items = items;
        updateMetadata(); // auto-refresh metadata when items are set
    }

    /**
     * @return SequencedCollection of actions for undo/redo functionality
     * Uses getLast() and removeLast() for stack-like LIFO behavior
     */
    public SequencedCollection<CartItem> getActionHistory() {
        return actionHistory;
    }

    public void setActionHistory(SequencedCollection<CartItem> actionHistory) {
        this.actionHistory = actionHistory;
    }

    // ✅ FIX #3: Metadata getters/setters
    // These are exposed for frontend visualization (used by shopping_cart.js)
    public Product getOldestItem() { return oldestItem; }
    public void setOldestItem(Product oldestItem) { this.oldestItem = oldestItem; }

    public Product getNewestItem() { return newestItem; }
    public void setNewestItem(Product newestItem) { this.newestItem = newestItem; }
}

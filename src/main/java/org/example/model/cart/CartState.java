package org.example.model.cart;

import java.util.ArrayList;
import java.util.SequencedCollection;
import java.math.BigDecimal;

/**
 * CartState - Demonstrates Java 21 Sequenced Collections Best Practices
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

    // Metadata for demonstrating Java 21 getFirst() and getLast()
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
     */
    public void updateMetadata() {
        if (!items.isEmpty()) {
            // ✅ Java 21 API: getFirst() - cleaner than get(0)
            this.oldestItem = items.getFirst().getProduct();

            // ✅ Java 21 API: getLast() - cleaner than get(size() - 1)
            this.newestItem = items.getLast().getProduct();
        } else {
            this.oldestItem = null;
            this.newestItem = null;
        }
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

    // ❌ MISTAKE:  Returning generic List in getters
    // public List<CartItem> getItems() { return items; }
    // public void setItems(List<CartItem> items) { this.items = items; }

    // ✅ FIX : Return SequencedCollection to preserve API contract
    // 💡 BEST PRACTICE: Match return type with field type for clarity
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
    }

    // ❌ MISTAKE : Action history should also be SequencedCollection
    // public List<CartItem> getActionHistory() { return actionHistory; }
    // public void setActionHistory(List<CartItem> actionHistory) { this.actionHistory = actionHistory; }

    // ✅ FIX : Return SequencedCollection for action history
    // 💡 BEST PRACTICE: History by nature is sequential - use SequencedCollection!
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

    // Metadata getters/setters - these are fine as-is
    public Product getOldestItem() { return oldestItem; }
    public void setOldestItem(Product oldestItem) { this.oldestItem = oldestItem; }

    public Product getNewestItem() { return newestItem; }
    public void setNewestItem(Product newestItem) { this.newestItem = newestItem; }
}

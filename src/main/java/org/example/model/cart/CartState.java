// Cart state for responses
package org.example.model.cart;

import java.util.List;
import java.util.ArrayList;

public class CartState {
    private List<CartItem> items = new ArrayList<>();
    private List<CartItem> actionHistory = new ArrayList<>(); // Simple history for undo
    private Product oldestItem;
    private Product newestItem;

    public CartState() {}

    // Calculate metadata based on current items using Java 21 Sequenced Collections
    public void updateMetadata() {
        if (!items.isEmpty()) {
            this.oldestItem = items.getFirst().getProduct(); // Java 21 getFirst()
            this.newestItem = items.getLast().getProduct();  // Java 21 getLast()
        } else {
            this.oldestItem = null;
            this.newestItem = null;
        }
    }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public List<CartItem> getActionHistory() { return actionHistory; }
    public void setActionHistory(List<CartItem> actionHistory) { this.actionHistory = actionHistory; }

    public Product getOldestItem() { return oldestItem; }
    public void setOldestItem(Product oldestItem) { this.oldestItem = oldestItem; }

    public Product getNewestItem() { return newestItem; }
    public void setNewestItem(Product newestItem) { this.newestItem = newestItem; }
}
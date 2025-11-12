package org.example.model.cart;

import java.math.BigDecimal;
import java.util.Random; // Added this import

/**
 * CartItem - Represents a single item in the shopping cart
 *
 * Design decisions:
 * - Final fields for immutability
 * - Unique ID for tracking individual cart entries
 * - Separate quantity and unit price
 */
public class CartItem {
    private final Long id;
    private final Product product;
    private final int quantity; // Changed to primitive int
    private final BigDecimal unitPrice;

    // Removed default constructor

    public CartItem(Long id, Product product, int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Generates unique ID for each cart item
     * In production, use UUID or database sequence
     */
    public static Long generateId() {
        // Updated to match slide version
        return System.currentTimeMillis() + new Random().nextInt(1000);
    }

    // --- All setters removed ---

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; } // Return type changed to int
    public BigDecimal getUnitPrice() { return unitPrice; }

    @Override
    public String toString() {
        // Assuming Product has a .getName() or you've changed it to a record
        return String.format("CartItem[id=%d, product=%s, qty=%d, price=%s]",
                id, product.name(), quantity, unitPrice);
    }
}
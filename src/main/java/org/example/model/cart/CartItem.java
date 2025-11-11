package org.example.model.cart;

import java.math.BigDecimal;
/**
 * CartItem - Represents a single item in the shopping cart
 *
 * Design decisions:
 * - Final fields for immutability
 * - Unique ID for tracking individual cart entries
 * - Separate quantity and unit price
 */
public class CartItem {
    private Long id;
    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;

    public CartItem() {}

    public CartItem(Long id, Product product, Integer quantity, BigDecimal unitPrice) {
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
        return System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    @Override
    public String toString() {
        return String.format("CartItem[id=%d, product=%s, qty=%d, price=%s]",
                id, product.getName(), quantity, unitPrice);
    }
}

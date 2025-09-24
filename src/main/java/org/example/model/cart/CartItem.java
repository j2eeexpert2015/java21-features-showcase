package org.example.model.cart;

import java.math.BigDecimal;

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

    // Generate unique ID for demo purposes
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
}

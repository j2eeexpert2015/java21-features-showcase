package org.example.model.cart;

import java.math.BigDecimal;

/**
 * Represents a single product in the TechMart catalog.
 * Using BigDecimal for price is essential for financial accuracy.
 * @param name
 * @param price
 */
public record Product(String name, BigDecimal price)
{
    /* Provides a user-friendly string for logging and display.
     * Example: "iPhone 15 ($999.99)"
     */
    @Override
    public String toString() { return name + " ($" + price + ")"; }
}

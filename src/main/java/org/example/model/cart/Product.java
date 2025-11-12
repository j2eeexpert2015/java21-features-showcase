package org.example.model.cart;

import java.math.BigDecimal;

public record Product(String name, BigDecimal price)
{
    @Override
    public String toString() { return name + " ($" + price + ")"; }
}

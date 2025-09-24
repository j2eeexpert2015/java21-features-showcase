// In-memory repository for demo
package org.example.repository;

import org.example.model.cart.CartItem;
import org.example.model.cart.CartState;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.HashMap;

@Repository
public class CartRepository {
    // Simple in-memory storage for demo purposes
    private final Map<Long, CartState> customerCarts = new HashMap<>();

    public CartState getCartState(Long customerId) {
        return customerCarts.computeIfAbsent(customerId, k -> new CartState());
    }

    public void saveCartState(Long customerId, CartState cartState) {
        customerCarts.put(customerId, cartState);
    }

    // Clear all data for demo reset
    public void clearAllCarts() {
        customerCarts.clear();
    }
}
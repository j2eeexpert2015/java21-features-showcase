// Service layer with Java 21 Sequenced Collections
package org.example.service;

import org.example.model.cart.*;
import org.example.repository.CartRepository;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    // Add item to end of cart using Java 21 addLast()
    public void addItem(Long customerId, CartItemRequest request) {
        logger.info("SERVICE: Adding item '{}' to cart for customer {}",
                request.getProductName(), customerId);
        CartState cartState = cartRepository.getCartState(customerId);
        // ✅ Log collection size before operation for debugging
        int sizeBefore = cartState.getItems().size();

        CartItem item = new CartItem(
                CartItem.generateId(),
                new Product(request.getProductName(), request.getPrice()),
                request.getQuantity(),
                request.getPrice()
        );

        cartState.getItems().addLast(item); // Java 21 Sequenced Collections
        cartState.getActionHistory().addLast(item); // Track for undo
        cartState.updateMetadata();

        cartRepository.saveCartState(customerId, cartState);
        logger.info("SERVICE: Item added successfully. Cart size: {} -> {}",
                sizeBefore, cartState.getItems().size());
    }

    // Add priority item to front using Java 21 addFirst()
    public void addPriorityItem(Long customerId, CartItemRequest request) {
        CartState cartState = cartRepository.getCartState(customerId);

        CartItem item = new CartItem(
                CartItem.generateId(),
                new Product(request.getProductName(), request.getPrice()),
                request.getQuantity(),
                request.getPrice()
        );

        cartState.getItems().addFirst(item); // Java 21 Sequenced Collections
        cartState.getActionHistory().addLast(item); // Track for undo
        cartState.updateMetadata();

        cartRepository.saveCartState(customerId, cartState);
    }

    // Remove specific item by ID
    public void removeItem(Long customerId, Long itemId) {
        CartState cartState = cartRepository.getCartState(customerId);

        cartState.getItems().removeIf(item -> item.getId().equals(itemId));
        cartState.updateMetadata();

        cartRepository.saveCartState(customerId, cartState);
    }

    // Undo last action using Java 21 removeLast()
    public void undoLastAction(Long customerId) {
        CartState cartState = cartRepository.getCartState(customerId);

        if (!cartState.getActionHistory().isEmpty()) {
            CartItem lastAdded = cartState.getActionHistory().removeLast(); // Java 21
            cartState.getItems().removeIf(item -> item.getId().equals(lastAdded.getId()));
            cartState.updateMetadata();

            cartRepository.saveCartState(customerId, cartState);
        }
    }

    // Clear entire cart
    public void clearCart(Long customerId) {
        CartState cartState = cartRepository.getCartState(customerId);

        cartState.getItems().clear();
        cartState.getActionHistory().clear();
        cartState.updateMetadata();

        cartRepository.saveCartState(customerId, cartState);
    }

    public CartState getCartState(Long customerId) {
        CartState cartState = cartRepository.getCartState(customerId);
        cartState.updateMetadata(); // Ensure metadata is current
        return cartState;
    }
}
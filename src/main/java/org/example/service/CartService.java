/*
 * CartService - Service Layer for Shopping Cart Operations
 *
 * Demonstrates Java 21 Sequenced Collections API in a real-world e-commerce scenario.
 *
 * Key Java 21 Features Used:
 * - SequencedCollection interface for explicit ordering guarantees
 * - addLast() / addFirst() for position-specific insertions
 * - getLast() for retrieving most recent items
 * - removeLast() for undo/LIFO behavior
 *
 * Design Pattern: Service Layer pattern separating business logic from controllers
 */
package org.example.service;

import org.example.dto.cart.CartItemRequest;
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

    /*
     * ADD ITEM TO END - Standard "Add to Cart" Operation
     *
     * Uses Java 21 addLast() to append item to end of cart.
     *
     * Why addLast() instead of add()?
     * - Makes intent explicit - "append to end"
     * - More readable than items.add()
     * - Part of SequencedCollection interface contract
     *
     * Business Logic:
     * 1. Retrieve current cart state
     * 2. Create new cart item
     * 3. Append to end using addLast() - maintains insertion order
     * 4. Track in action history for undo functionality
     * 5. Update metadata (oldest/newest items)
     * 6. Persist changes
     */
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

        // JAVA 21 API: addLast() - Explicitly add to END of collection
        cartState.getItems().addLast(item);

        // Track in chronological history for undo functionality
        cartState.getActionHistory().addLast(item);

        cartState.updateMetadata();
        cartRepository.saveCartState(customerId, cartState);

        logger.info("SERVICE: Item added successfully. Cart size: {} -> {}",
                sizeBefore, cartState.getItems().size());
    }

    /*
     * ADD PRIORITY ITEM TO FRONT - Express/Rush Item Handling
     *
     * Uses Java 21 addFirst() to insert item at FRONT of cart.
     *
     * Why addFirst()?
     * - Priority items (express shipping, rush orders) go to front
     * - Ensures priority items are processed first
     * - Clearer than items.add(0, item) - explicit about position
     *
     * Use Case: Customer pays for express shipping - item jumps queue
     */
    public void addPriorityItem(Long customerId, CartItemRequest request) {
        logger.info("SERVICE: Adding PRIORITY item '{}' for customer {}",
                request.getProductName(), customerId);

        CartState cartState = cartRepository.getCartState(customerId);
        CartItem item = new CartItem(
                CartItem.generateId(),
                new Product(request.getProductName(), request.getPrice()),
                request.getQuantity(),
                request.getPrice()
        );

        // JAVA 21 API: addFirst() - Insert at FRONT of collection (position 0)
        cartState.getItems().addFirst(item);

        // Still track chronologically in history (not at front)
        cartState.getActionHistory().addLast(item);

        cartState.updateMetadata();
        cartRepository.saveCartState(customerId, cartState);
        int sizeBefore = cartState.getItems().size();
        logger.info("SERVICE: Priority item added to FRONT. Cart size: {} -> {}",
                sizeBefore, cartState.getItems().size());
    }

    /*
     * REMOVE SPECIFIC ITEM - Remove by ID
     *
     * Removes a specific item from cart using standard removeIf().
     * This is NOT a Java 21 feature - included for completeness.
     *
     * Note: Could use removeFirst()/removeLast() if removing by position,
     * but this removes by ID which requires removeIf()
     */
    public void removeItem(Long customerId, Long itemId) {
        logger.info("SERVICE: Removing item ID {} for customer {}", itemId, customerId);

        CartState cartState = cartRepository.getCartState(customerId);

        boolean removed = cartState.getItems().removeIf(item -> item.getId().equals(itemId));

        if (removed) {
            cartState.updateMetadata();
            cartRepository.saveCartState(customerId, cartState);
            logger.info("SERVICE: Item {} removed successfully", itemId);
        } else {
            logger.warn("SERVICE: Item {} not found in cart", itemId);
        }
    }

    /*
     * UNDO LAST ACTION - Implements Undo Functionality
     *
     * Uses Java 21 getLast() + removeLast() for stack-like LIFO behavior.
     *
     * Why getLast() then removeLast()?
     * - Demonstrates TWO Java 21 APIs clearly (educational!)
     * - getLast() = peek at most recent (read-only)
     * - removeLast() = pop from stack (destructive)
     * - Shows stack pattern: Last In, First Out (LIFO)
     *
     * Pattern: Action History as Stack
     * - addLast() when adding items (push)
     * - getLast() to see what to undo (peek)
     * - removeLast() to undo (pop)
     */
    public void undoLastAction(Long customerId) {
        logger.info("SERVICE: Undoing last action for customer {}", customerId);

        CartState cartState = cartRepository.getCartState(customerId);

        if (!cartState.getActionHistory().isEmpty()) {

            // JAVA 21 API: getLast() - Peek at most recent action (non-destructive)
            CartItem lastAdded = cartState.getActionHistory().getLast();
            String itemName = lastAdded.getProduct().name();

            // JAVA 21 API: removeLast() - Remove most recent action from history (destructive)
            cartState.getActionHistory().removeLast();

            cartState.getItems().removeIf(item -> item.getId().equals(lastAdded.getId()));

            cartState.updateMetadata();
            cartRepository.saveCartState(customerId, cartState);

            logger.info("SERVICE: Undo successful - removed '{}'", itemName);
        } else {
            logger.warn("SERVICE: Cannot undo - no actions in history");
        }
    }

    /*
     * CLEAR CART - Remove All Items
     *
     * Standard clear() method - not Java 21 specific but part of Collection API.
     * Included for completeness of cart operations.
     */
    public void clearCart(Long customerId) {
        logger.info("SERVICE: Clearing cart for customer {}", customerId);

        CartState cartState = cartRepository.getCartState(customerId);
        int itemCount = cartState.getItems().size();

        cartState.getItems().clear();
        cartState.getActionHistory().clear();
        cartState.updateMetadata();

        cartRepository.saveCartState(customerId, cartState);

        logger.info("SERVICE: Cart cleared - removed {} items", itemCount);
    }

    /*
     * GET CART STATE - Retrieve Current Cart
     *
     * Simple getter with metadata refresh.
     * Called by controller for UI updates after operations.
     *
     * Note: Metadata update internally uses getFirst() and getLast() (Java 21 APIs)
     */
    public CartState getCartState(Long customerId) {
        logger.debug("SERVICE: Fetching cart state for customer {}", customerId);

        CartState cartState = cartRepository.getCartState(customerId);
        cartState.updateMetadata();

        return cartState;
    }
}
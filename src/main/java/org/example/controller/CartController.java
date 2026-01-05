package org.example.controller;

import org.example.dto.cart.CartItemRequest;
import org.example.dto.common.ApiResponse;
import org.example.model.cart.*;
import org.example.constants.Java21Methods;
import org.example.service.CartService;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Add regular item to cart end
    @PostMapping("/{customerId}/addlastitem")
    public ApiResponse addItem(@PathVariable Long customerId, @RequestBody CartItemRequest request) {
        logger.info(">>> Received request to add item: {} for customer: {}", request.getProductName(), customerId);

        cartService.addItem(customerId, request);

        logger.info(">>> Item added successfully, returning response");

        return new ApiResponse("ShoppingCartController.addItem",
                "Item added to end of cart using Sequenced Collection")
                .withServiceCall("CartService.addItem", List.of(Java21Methods.ADD_LAST));
    }

    // Add priority item to cart front
    @PostMapping("/{customerId}/addfirstitem")
    public ApiResponse addPriorityItem(@PathVariable Long customerId, @RequestBody CartItemRequest request) {
        logger.info(">>> Received request to add priority item: {} for customer: {}", request.getProductName(), customerId);

        cartService.addPriorityItem(customerId, request);

        logger.info(">>> Priority item added successfully");

        return new ApiResponse("ShoppingCartController.addPriorityItem",
                "Priority item added to front of cart using Sequenced Collection")
                .withServiceCall("CartService.addPriorityItem", List.of(Java21Methods.ADD_FIRST));
    }

    // Remove specific item
    @DeleteMapping("/{customerId}/items/{itemId}")
    public ApiResponse removeItem(@PathVariable Long customerId, @PathVariable Long itemId) {
        logger.info(">>> Received request to remove item: {} for customer: {}", itemId, customerId);

        cartService.removeItem(customerId, itemId);

        return new ApiResponse("ShoppingCartController.removeItem",
                "Item removed from cart")
                .withServiceCall("CartService.removeItem", List.of(Java21Methods.REMOVE));
    }

    // Undo last addition
    @PostMapping("/{customerId}/removelastitem")
    public ApiResponse undoLastAction(@PathVariable Long customerId) {
        logger.info(">>> Received request to undo last action for customer: {}", customerId);

        cartService.undoLastAction(customerId);

        return new ApiResponse("ShoppingCartController.undoLastAction",
                "Last action undone using Sequenced Collection removeLast")
                .withServiceCall("CartService.undoLastAction", List.of(Java21Methods.GET_LAST, Java21Methods.REMOVE_LAST));
    }

    // Clear entire cart
    @DeleteMapping("/{customerId}")
    public ApiResponse clearCart(@PathVariable Long customerId) {
        logger.info(">>> Received request to clear cart for customer: {}", customerId);

        cartService.clearCart(customerId);

        return new ApiResponse("ShoppingCartController.clearCart",
                "All items removed from cart")
                .withServiceCall("CartService.clearCart", List.of(Java21Methods.CLEAR));
    }

    // Get current cart state (for UI updates)
    @GetMapping("/{customerId}")
    public CartState getCart(@PathVariable Long customerId) {
        logger.info(">>> Received request to get cart state for customer: {}", customerId);

        return cartService.getCartState(customerId);
    }
}
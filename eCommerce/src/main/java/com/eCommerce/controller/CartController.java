package com.eCommerce.controller;

import com.eCommerce.dto.AddCartRequestDTO;
import com.eCommerce.dto.CartDTO;
import com.eCommerce.dto.CartItemDTO;
import com.eCommerce.dto.ResponseWrapper;
import com.eCommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user shopping carts.
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Retrieves a user's cart. Returns an empty cart if the user has no items.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseWrapper<CartDTO>> getCart(@PathVariable Long userId) {
        CartDTO cart = cartService.getCart(userId);
        return ResponseEntity.ok(new ResponseWrapper<>(cart));
    }

    /**
     * Wipes all items from a user's cart.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> wipeCart(@PathVariable Long userId) {
        cartService.wipeCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a snapshot of a product to the user's cart.
     */
    @PostMapping("/{userId}/items")
    public ResponseEntity<ResponseWrapper<CartItemDTO>> addItemToCart(
            @PathVariable Long userId,
            @Valid @RequestBody AddCartRequestDTO request) {
        CartItemDTO createdItem = cartService.addItemToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseWrapper<>(createdItem));
    }

    /**
     * Deletes a specific item from the user's cart.
     */
    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {
        cartService.removeItemFromCart(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }
}
package com.eCommerce.service;

import com.eCommerce.dto.AddCartRequestDTO;
import com.eCommerce.dto.CartDTO;
import com.eCommerce.dto.CartItemDTO;
import com.eCommerce.entity.CartItem;
import com.eCommerce.entity.Product;
import com.eCommerce.exception.ResourceNotFoundException;
import com.eCommerce.repository.CartItemRepository;
import com.eCommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service handling business logic for the shopping cart.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    /**
     * Retrieves the cart for a user, calculating the total price dynamically.
     * Returns an empty cart if the user has no items.
     */
    @Transactional(readOnly = true)
    public CartDTO getCart(Long userId) {
        log.info("Fetching cart for user ID: {}", userId);
        List<CartItem> items = cartItemRepository.findByUserIdOrderByAddedAtDescIdDesc(userId);

        List<CartItemDTO> itemDtos = items.stream()
                .map(this::mapToDto)
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(CartItem::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDTO(userId, itemDtos, totalPrice);
    }

    /**
     * Adds a product to the user's cart, taking a snapshot of its current price and title.
     */
    @Transactional
    public CartItemDTO addItemToCart(Long userId, AddCartRequestDTO request) {
        log.info("Adding product ID: {} to cart for user ID: {}", request.productId(), userId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> {
                    log.warn("Product with ID: {} not found", request.productId());
                    return new ResourceNotFoundException("Product with ID " + request.productId() + " not found");
                });

        CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(product.getId())
                .title(product.getTitle())
                .unitPrice(product.getPrice()) // Snapshot taken here
                .build();

        CartItem savedItem = cartItemRepository.save(cartItem);
        log.info("Successfully added item to cart. CartItem ID: {}", savedItem.getId());

        return mapToDto(savedItem);
    }

    /**
     * Removes a specific item from the user's cart.
     */
    @Transactional
    public void removeItemFromCart(Long userId, Long cartItemId) {
        log.info("Removing cart item ID: {} for user ID: {}", cartItemId, userId);

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> {
                    log.warn("Cart item ID: {} not found for user ID: {}", cartItemId, userId);
                    return new ResourceNotFoundException("Cart item not found in user's cart");
                });

        cartItemRepository.delete(cartItem);
        log.info("Successfully removed cart item ID: {}", cartItemId);
    }

    /**
     * Wipes all items from a user's cart.
     */
    @Transactional
    public void wipeCart(Long userId) {
        log.info("Wiping cart for user ID: {}", userId);
        cartItemRepository.deleteByUserId(userId);
    }

    /**
     * Helper method to map entity to DTO.
     */
    private CartItemDTO mapToDto(CartItem item) {
        return new CartItemDTO(
                item.getId(),
                item.getProductId(),
                item.getTitle(),
                item.getUnitPrice(),
                item.getAddedAt()
        );
    }
}
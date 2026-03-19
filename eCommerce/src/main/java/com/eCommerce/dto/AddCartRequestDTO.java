package com.eCommerce.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for adding a product to the cart.
 */
public record AddCartRequestDTO(
        @NotNull(message = "productId is required")
        Long productId
) {}
package com.eCommerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for adding a product to the cart.
 */
public record AddCartRequestDTO(
        @NotNull(message = "productId is required")
        @Positive(message = "productId must be a positive number")
        Long productId
) {}
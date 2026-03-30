package com.eCommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Data Transfer Object for creating a new product.
 */
public record CreateProductRequestDTO(
        @NotNull(message = "sellerId is required")
        @Positive(message = "sellerId must be a positive number")
        Long sellerId,

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "price is required")
        @Positive(message = "price must be a positive number")
        @DecimalMin(value = "0.01", message = "price must be at least 0.01")
        BigDecimal price
) {}
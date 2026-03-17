package com.eCommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a product response.
 */
public record ProductDTO(
        Long id,
        Long sellerId,
        String title,
        String description,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
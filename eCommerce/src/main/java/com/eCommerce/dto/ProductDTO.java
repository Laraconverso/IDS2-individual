package com.eCommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER, pattern = "0.00")
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
package com.eCommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a single item in the cart.
 */
public record CartItemDTO(
        Long id,
        Long productId,
        String title,
        @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER, pattern = "0.00")
        BigDecimal unitPrice,
        LocalDateTime addedAt
) {}
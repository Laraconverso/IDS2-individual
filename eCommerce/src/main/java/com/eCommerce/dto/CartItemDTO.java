package com.eCommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a single item in the cart.
 */
public record CartItemDTO(
        Long id,
        Long productId,
        String title,
        BigDecimal unitPrice,
        LocalDateTime addedAt
) {}
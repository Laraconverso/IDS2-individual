package com.eCommerce.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing the aggregated user cart.
 */
public record CartDTO(
        Long userId,
        List<CartItemDTO> items,
        BigDecimal totalPrice
) {}

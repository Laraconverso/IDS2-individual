package com.eCommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing the aggregated user cart.
 */
public record CartDTO(
        Long userId,
        List<CartItemDTO> items,
        @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER, pattern = "0.00")
        BigDecimal totalPrice
) {}

package com.eCommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a snapshot of a product added to a user's cart.
 * Maps to the "cart_items" table.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    /**
     * Unique identifier for the cart item.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who owns this cart item.
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * The ID of the original product.
     */
    @Column(nullable = false)
    private Long productId;

    /**
     * Snapshot of the product's title at the time it was added.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Snapshot of the product's price at the time it was added.
     * Uses NUMERIC(19,2) to support large monetary values without overflow.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Timestamp when the item was added to the cart.
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime addedAt;


    @PrePersist
    @PreUpdate
    public void formatPrice() {
        if (this.unitPrice != null) {
            this.unitPrice = this.unitPrice.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
package com.eCommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a product in the eCommerce catalog.
 * Mapped to the "products" table in the PostgreSQL database.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /**
     * Unique identifier for the product.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifier of the seller who listed the product.
     */
    @Column(nullable = false)
    private Long sellerId;

    /**
     * Title or name of the product.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Detailed description of the product.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Price of the product in USD. Must be at least 0.01.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Timestamp when the product was added to the catalog.
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the product details were last updated.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
package com.eCommerce.repository;

import com.eCommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link Product} entity.
 * Provides basic CRUD operations mapping to the underlying database.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
package com.eCommerce.repository;

import com.eCommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link CartItem} entities.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Retrieves all items for a specific user, ordered exactly as required by the OpenAPI spec:
     * by addedAt descending, then by id descending.
     */
    List<CartItem> findByUserIdOrderByAddedAtDescIdDesc(Long userId);

    /**
     * Finds a specific cart item belonging to a specific user.
     */
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    /**
     * Deletes all cart items belonging to a specific user (Wipe cart).
     */
    void deleteByUserId(Long userId);

    /**
     * Deletes all cart items associated with a specific product ID.
     * Useful for the "Borrado de producto" cascading requirement.
     */
    void deleteByProductId(Long productId);
}
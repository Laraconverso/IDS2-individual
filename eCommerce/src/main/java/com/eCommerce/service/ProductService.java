package com.eCommerce.service;

import com.eCommerce.dto.CreateProductRequestDTO;
import com.eCommerce.dto.ProductDTO;
import com.eCommerce.dto.UpdateProductRequestDTO;
import com.eCommerce.entity.Product;
import com.eCommerce.exception.ResourceNotFoundException;
import com.eCommerce.repository.CartItemRepository;
import com.eCommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stateless service handling business logic for products.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    /**
     * Creates a new product in the catalog.
     */
    @Transactional
    public ProductDTO createProduct(CreateProductRequestDTO request) {
        log.info("Creating new product with title: {}", request.title());

        Product product = Product.builder()
                .sellerId(request.sellerId())
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return mapToDto(savedProduct);
    }

    /**
     * Retrieves all products ordered by ID ascending.
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.info("Retrieving all products");
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Retrieves a single product by its ID.
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = findProductEntityById(id);
        return mapToDto(product);
    }

    /**
     * Fully updates an existing product.
     */
    @Transactional
    public ProductDTO updateProduct(Long id, UpdateProductRequestDTO request) {
        log.info("Updating product with ID: {}", id);
        Product product = findProductEntityById(id);

        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());

        Product updatedProduct = productRepository.save(product);
        log.info("Product with ID: {} updated successfully", id);

        return mapToDto(updatedProduct);
    }

    /**
     * Deletes a product by its ID.
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        Product product = findProductEntityById(id);
        cartItemRepository.deleteByProductId(id);
        productRepository.delete(product);
        log.info("Product with ID: {} deleted successfully", id);
    }

    /**
     * Helper method to find a product or throw a 404 exception.
     */
    private Product findProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product with ID: {} not found", id);
                    return new ResourceNotFoundException("Product with ID " + id + " not found");
                });
    }

    /**
     * Helper method to map a Product entity to a ProductDTO.
     */
    private ProductDTO mapToDto(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
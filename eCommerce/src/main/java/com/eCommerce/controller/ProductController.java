package com.eCommerce.controller;

import com.eCommerce.dto.CreateProductRequestDTO;
import com.eCommerce.dto.ProductDTO;
import com.eCommerce.dto.ResponseWrapper;
import com.eCommerce.dto.UpdateProductRequestDTO;
import com.eCommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing products in the eCommerce platform.
 * Exposes standard CRUD operations mapping to the /products endpoint.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Creates a new product.
     */
    @PostMapping
    public ResponseEntity<ResponseWrapper<ProductDTO>> createProduct(@Valid @RequestBody CreateProductRequestDTO request) {
        ProductDTO createdProduct = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseWrapper<>(createdProduct));
    }

    /**
     * Retrieves all products.
     */
    @GetMapping
    public ResponseEntity<ResponseWrapper<List<ProductDTO>>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(new ResponseWrapper<>(products));
    }

    /**
     * Retrieves a single product by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<ProductDTO>> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(new ResponseWrapper<>(product));
    }

    /**
     * Fully updates an existing product.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseWrapper<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequestDTO request) {
        ProductDTO updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.ok(new ResponseWrapper<>(updatedProduct));
    }

    /**
     * Deletes a product by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // 204 No Content, as requested
    }
}
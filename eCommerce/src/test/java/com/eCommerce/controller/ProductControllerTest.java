package com.eCommerce.controller;

import com.eCommerce.dto.CreateProductRequestDTO;
import com.eCommerce.dto.UpdateProductRequestDTO;
import com.eCommerce.entity.Product;
import com.eCommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * End-to-End integration tests for the Product API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /products - Success: Create product and return 201 with all fields")
    void createProduct_Success() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Notebook", "High-end laptop", new BigDecimal("1500.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.sellerId", is(100)))
                .andExpect(jsonPath("$.data.title", is("Notebook")))
                .andExpect(jsonPath("$.data.description", is("High-end laptop")))
                .andExpect(jsonPath("$.data.price", is(1500.00)))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.updatedAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /products - Validation: Invalid price (negative) returns 400 RFC 7807")
    void createProduct_InvalidPrice_Returns400() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Notebook", "Description", new BigDecimal("-10.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", containsString("price must be at least 0.01")))
                .andExpect(jsonPath("$.instance", containsString("/products")));
    }

    @Test
    @DisplayName("POST /products - Validation: Missing title returns 400")
    void createProduct_MissingTitle_Returns400() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "", "Description", new BigDecimal("100.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("title")));
    }

    @Test
    @DisplayName("GET /products - Success: Return all products ordered by id ascending")
    void getAllProducts_Success() throws Exception {
        Product p1 = Product.builder()
                .sellerId(1L).title("Product A").description("Desc A").price(new BigDecimal("100.00"))
                .build();
        Product p2 = Product.builder()
                .sellerId(2L).title("Product B").description("Desc B").price(new BigDecimal("200.00"))
                .build();
        productRepository.save(p1);
        productRepository.save(p2);

        mockMvc.perform(get("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", isA(java.util.List.class)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title", is("Product A")))
                .andExpect(jsonPath("$.data[1].title", is("Product B")));
    }

    @Test
    @DisplayName("GET /products - Empty list when no products exist")
    void getAllProducts_Empty() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /products/{id} - Success: Return product by ID")
    void getProductById_Success() throws Exception {
        Product product = Product.builder()
                .sellerId(1L).title("Mouse").description("Wireless").price(new BigDecimal("25.00"))
                .build();
        product = productRepository.save(product);

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(product.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Mouse")))
                .andExpect(jsonPath("$.data.price", is(25.00)));
    }

    @Test
    @DisplayName("GET /products/{id} - Error 404: Non-existent product returns RFC 7807")
    void getProductById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Product Not Found")))
                .andExpect(jsonPath("$.detail", containsString("Product with ID 9999 not found")))
                .andExpect(jsonPath("$.instance", containsString("/products/9999")));
    }

    @Test
    @DisplayName("PUT /products/{id} - Success: Update all fields and return 200")
    void updateProduct_Success() throws Exception {
        Product product = Product.builder()
                .sellerId(1L).title("Old Title").description("Old Desc").price(new BigDecimal("100.00"))
                .build();
        product = productRepository.save(product);

        UpdateProductRequestDTO request = new UpdateProductRequestDTO(
                "New Title", "New Description", new BigDecimal("150.00")
        );

        mockMvc.perform(put("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(product.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("New Title")))
                .andExpect(jsonPath("$.data.description", is("New Description")))
                .andExpect(jsonPath("$.data.price", is(150.00)));
    }

    @Test
    @DisplayName("PUT /products/{id} - Validation: Invalid price returns 400")
    void updateProduct_InvalidPrice_Returns400() throws Exception {
        Product product = Product.builder()
                .sellerId(1L).title("Title").description("Desc").price(new BigDecimal("100.00"))
                .build();
        product = productRepository.save(product);

        UpdateProductRequestDTO request = new UpdateProductRequestDTO(
                "Title", "Desc", new BigDecimal("0.00")
        );

        mockMvc.perform(put("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("price must be at least 0.01")));
    }

    @Test
    @DisplayName("PUT /products/{id} - Error 404: Update non-existent product returns 404")
    void updateProduct_NotFound_Returns404() throws Exception {
        UpdateProductRequestDTO request = new UpdateProductRequestDTO(
                "Title", "Desc", new BigDecimal("100.00")
        );

        mockMvc.perform(put("/products/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("Product with ID 9999 not found")));
    }

    @Test
    @DisplayName("DELETE /products/{id} - Success: Delete product and return 204")
    void deleteProduct_Success() throws Exception {
        Product product = Product.builder()
                .sellerId(1L).title("Mouse").description("Wireless").price(new BigDecimal("25.00"))
                .build();
        product = productRepository.save(product);

        mockMvc.perform(delete("/products/" + product.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /products/{id} - Error 404: Delete non-existent product returns 404")
    void deleteProduct_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("Product with ID 9999 not found")));
    }
}
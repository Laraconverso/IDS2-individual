package com.eCommerce.controller;

import com.eCommerce.dto.CreateProductRequestDTO;
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
        // Limpiamos la base de datos antes de cada test para que sean independientes
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /products - Success: Should create a product and return 201")
    void createProduct_Success() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Notebook", "High-end laptop", new BigDecimal("1500.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title", is("Notebook")))
                .andExpect(jsonPath("$.data.price", is(1500.00)));
    }

    @Test
    @DisplayName("POST /products - Error 400: Should return RFC 7807 format for invalid price")
    void createProduct_InvalidPrice_Returns400() throws Exception {
        // Precio negativo, viola el @DecimalMin("0.01")
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
                .andExpect(jsonPath("$.detail", containsString("price must be at least 0.01")));
    }

    @Test
    @DisplayName("GET /products/{id} - Error 404: Should return RFC 7807 when product not found")
    void getProductById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Product Not Found")))
                .andExpect(jsonPath("$.detail", containsString("Product with ID 9999 not found")));
    }

    @Test
    @DisplayName("DELETE /products/{id} - Success: Should delete product and return 204")
    void deleteProduct_Success() throws Exception {
        // Primero guardamos un producto real en la DB
        Product product = Product.builder()
                .sellerId(1L).title("Mouse").description("Wireless").price(new BigDecimal("25.00"))
                .build();
        product = productRepository.save(product);

        // Luego lo borramos mediante la API
        mockMvc.perform(delete("/products/" + product.getId()))
                .andExpect(status().isNoContent());

        // Verificamos que realmente se borró de la base de datos
        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isNotFound());
    }
}
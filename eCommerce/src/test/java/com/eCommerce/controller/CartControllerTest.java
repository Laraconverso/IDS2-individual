package com.eCommerce.controller;

import com.eCommerce.dto.AddCartRequestDTO;
import com.eCommerce.entity.CartItem;
import com.eCommerce.entity.Product;
import com.eCommerce.repository.CartItemRepository;
import com.eCommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;


import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End integration tests for the Cart API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private Product testProduct;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        productRepository.deleteAll();

        // Creamos un producto de prueba en la BD para poder agregarlo al carrito
        testProduct = Product.builder()
                .sellerId(100L)
                .title("Teclado Mecánico")
                .description("Teclado RGB")
                .price(new BigDecimal("50.00"))
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - E2E: Should add product and take price snapshot")
    void addItemToCart_Success() throws Exception {
        AddCartRequestDTO request = new AddCartRequestDTO(testProduct.getId());

        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.productId", is(testProduct.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Teclado Mecánico")))
                .andExpect(jsonPath("$.data.unitPrice", is(50.00)));
    }

    @Test
    @DisplayName("GET /cart/{userId} - E2E: Should return cart with calculated total price")
    void getCart_WithItems_ReturnsCalculatedTotal() throws Exception {
        // Agregamos el producto dos veces al carrito manualmente en la BD
        CartItem item1 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("T1").unitPrice(new BigDecimal("10.00")).build();
        CartItem item2 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("T2").unitPrice(new BigDecimal("20.50")).build();
        cartItemRepository.save(item1);
        cartItemRepository.save(item2);

        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(USER_ID.intValue())))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                // 10.00 + 20.50 = 30.50
                .andExpect(jsonPath("$.data.totalPrice", is(30.50)));
    }

    @Test
    @DisplayName("GET /cart/{userId} - E2E: Unknown user should return empty cart with total 0")
    void getCart_EmptyCart_ReturnsTotalZero() throws Exception {
        mockMvc.perform(get("/cart/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(999)))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalPrice", is(0)));
    }

    @Test
    @DisplayName("E2E CASCADE DELETE: Deleting a product must remove it from all carts")
    void productDeletion_CascadesToCartItems() throws Exception {
        // 1. Agregamos el ítem al carrito
        CartItem item = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item a borrar").unitPrice(new BigDecimal("50.00")).build();
        cartItemRepository.save(item);

        // 2. Simulamos la llamada a la API de PRODUCTOS para borrar el producto
        mockMvc.perform(delete("/products/" + testProduct.getId()))
                .andExpect(status().isNoContent());

        // 3. Verificamos en la API del CARRITO que el ítem desapareció automáticamente
        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalPrice", is(0)));
    }
}
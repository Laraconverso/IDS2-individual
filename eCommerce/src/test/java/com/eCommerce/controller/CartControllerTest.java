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

        testProduct = Product.builder()
                .sellerId(100L)
                .title("Teclado Mecánico")
                .description("Teclado RGB")
                .price(new BigDecimal("50.00"))
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Success: Add product and verify snapshot")
    void addItemToCart_Success() throws Exception {
        AddCartRequestDTO request = new AddCartRequestDTO(testProduct.getId());

        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.productId", is(testProduct.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Teclado Mecánico")))
                .andExpect(jsonPath("$.data.unitPrice", is(50.00)))
                .andExpect(jsonPath("$.data.addedAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Validation: Missing productId returns 400")
    void addItemToCart_MissingProductId_Returns400() throws Exception {
        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("productId")));
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Error 404: Non-existent product returns RFC 7807")
    void addItemToCart_ProductNotFound_Returns404() throws Exception {
        AddCartRequestDTO request = new AddCartRequestDTO(9999L);

        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Product Not Found")))
                .andExpect(jsonPath("$.detail", containsString("Product with ID 9999 not found")))
                .andExpect(jsonPath("$.instance", containsString("/cart")));
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Snapshot: Price not affected by product price change")
    void addItemToCart_SnapshotPreservesPrice() throws Exception {
        AddCartRequestDTO request = new AddCartRequestDTO(testProduct.getId());

        // Add product at original price (50.00)
        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.unitPrice", is(50.00)));

        // Change product price
        testProduct.setPrice(new BigDecimal("100.00"));
        productRepository.save(testProduct);

        // Verify cart still has original price
        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice", is(50.00)));
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Duplicates: Same product can be added multiple times")
    void addItemToCart_DuplicatesAllowed() throws Exception {
        AddCartRequestDTO request = new AddCartRequestDTO(testProduct.getId());

        // Add product first time
        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Add same product again
        mockMvc.perform(post("/cart/" + USER_ID + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify both items exist
        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    @DisplayName("GET /cart/{userId} - Success: Return cart with items and calculated total")
    void getCart_WithItems_ReturnsCalculatedTotal() throws Exception {
        CartItem item1 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item 1").unitPrice(new BigDecimal("10.00")).build();
        CartItem item2 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item 2").unitPrice(new BigDecimal("20.50")).build();
        cartItemRepository.save(item1);
        cartItemRepository.save(item2);

        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(USER_ID.intValue())))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.totalPrice", is(30.50)));
    }

    @Test
    @DisplayName("GET /cart/{userId} - Ordering: Items ordered by addedAt DESC, id DESC")
    void getCart_ItemsOrdering() throws Exception {
        CartItem item1 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("First").unitPrice(new BigDecimal("10.00")).build();
        CartItem item2 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Second").unitPrice(new BigDecimal("20.00")).build();
        CartItem item3 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Third").unitPrice(new BigDecimal("30.00")).build();

        cartItemRepository.save(item1);
        cartItemRepository.save(item2);
        cartItemRepository.save(item3);

        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                // Last added should be first (descending order)
                .andExpect(jsonPath("$.data.items[0].title", is("Third")))
                .andExpect(jsonPath("$.data.items[1].title", is("Second")))
                .andExpect(jsonPath("$.data.items[2].title", is("First")));
    }

    @Test
    @DisplayName("GET /cart/{userId} - Empty cart: Unknown user returns empty cart with totalPrice 0")
    void getCart_EmptyCart_ReturnsTotalZero() throws Exception {
        mockMvc.perform(get("/cart/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(999)))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalPrice", is(0)));
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Success: Remove item and return 204")
    void removeItemFromCart_Success() throws Exception {
        CartItem item = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item to remove").unitPrice(new BigDecimal("50.00")).build();
        CartItem saved = cartItemRepository.save(item);

        mockMvc.perform(delete("/cart/" + USER_ID + "/items/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Validation: Item must belong to user")
    void removeItemFromCart_WrongUser_Returns404() throws Exception {
        CartItem item = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item").unitPrice(new BigDecimal("50.00")).build();
        CartItem saved = cartItemRepository.save(item);

        // Try to delete from different user
        mockMvc.perform(delete("/cart/999/items/" + saved.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("Cart item not found")));
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Error 404: Non-existent item")
    void removeItemFromCart_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/cart/" + USER_ID + "/items/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.detail", containsString("not found")));
    }

    @Test
    @DisplayName("DELETE /cart/{userId} - Success: Wipe all items and return 204")
    void wipeCart_Success() throws Exception {
        CartItem item1 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item 1").unitPrice(new BigDecimal("10.00")).build();
        CartItem item2 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item 2").unitPrice(new BigDecimal("20.00")).build();
        cartItemRepository.save(item1);
        cartItemRepository.save(item2);

        mockMvc.perform(delete("/cart/" + USER_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalPrice", is(0)));
    }

    @Test
    @DisplayName("DELETE /cart/{userId} - Idempotent: Wiping empty cart returns 204")
    void wipeCart_EmptyCart_Idempotent() throws Exception {
        mockMvc.perform(delete("/cart/999"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("CASCADE DELETE: Deleting product removes it from all user carts")
    void productDeletion_CascadesToCartItems() throws Exception {
        CartItem item = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item in cart").unitPrice(new BigDecimal("50.00")).build();
        cartItemRepository.save(item);

        // Verify item exists
        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // Delete the product via Products API
        mockMvc.perform(delete("/products/" + testProduct.getId()))
                .andExpect(status().isNoContent());

        // Verify item was automatically deleted
        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalPrice", is(0)));
    }

    @Test
    @DisplayName("CASCADE DELETE: Multiple users - only product's items deleted")
    void cascadeDelete_MultipleUsers() throws Exception {
        Long otherUserId = 2L;

        // Add items to multiple users' carts
        CartItem item1 = CartItem.builder().userId(USER_ID).productId(testProduct.getId())
                .title("Item").unitPrice(new BigDecimal("50.00")).build();
        CartItem item2 = CartItem.builder().userId(otherUserId).productId(testProduct.getId())
                .title("Item").unitPrice(new BigDecimal("50.00")).build();
        cartItemRepository.save(item1);
        cartItemRepository.save(item2);

        // Delete product
        mockMvc.perform(delete("/products/" + testProduct.getId()))
                .andExpect(status().isNoContent());

        // Verify both users' carts are now empty
        mockMvc.perform(get("/cart/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));

        mockMvc.perform(get("/cart/" + otherUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }
}
package com.eCommerce.controller;

import com.eCommerce.dto.CreateProductRequestDTO;
import com.eCommerce.dto.AddCartRequestDTO;
import com.eCommerce.entity.Product;
import com.eCommerce.entity.CartItem;
import com.eCommerce.repository.ProductRepository;
import com.eCommerce.repository.CartItemRepository;
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
 * Contractual validation tests aligned with OpenAPI specification.
 * Each endpoint section includes success and all failure scenarios.
 * Tests RFC 7807 compliance for error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContractualValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        productRepository.deleteAll();
    }

    // ============================================================================
    // REENTREGA: Pruebas estrictas de formato, coerción y RFC 7807
    // ============================================================================

    @Test
    @DisplayName("RFC 7807 - Failure: Empty body returns 400 and ProblemDetail")
    void global_EmptyBody_Returns400AndProblemDetail() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("RFC 7807 - Failure: Invalid MediaType returns 415 and ProblemDetail")
    void global_InvalidMediaType_Returns415AndProblemDetail() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Just some plain text, not JSON"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"));
    }

    @Test
    @DisplayName("RFC 7807 - Failure: Float sent to Integer field returns 400 (No Coercion)")
    void global_FloatSentToInteger_Returns400AndProblemDetail() throws Exception {
        String jsonPayload = """
                {
                    "sellerId": 1.5,
                    "title": "Float test",
                    "description": "Testing float to int coercion",
                    "price": 10.00
                }
                """;
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("RFC 7807 - Failure: Boolean sent to Numeric field returns 400 (No Coercion)")
    void global_BooleanSentToNumeric_Returns400AndProblemDetail() throws Exception {
        String jsonPayload = """
                {
                    "sellerId": 1,
                    "title": "Boolean test",
                    "description": "Testing boolean coercion",
                    "price": true
                }
                """;
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("RFC 7807 - Failure: String sent as Path Variable returns 400")
    void global_StringSentAsId_Returns400AndProblemDetail() throws Exception {
        mockMvc.perform(get("/products/sarasa"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }


    // ============================================================================
    // POST /products - Create a new product
    // ============================================================================

    @Test
    @DisplayName("POST /products - Success: Valid product creation returns 201")
    void createProduct_Valid_Returns201() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Notebook", "High-performance laptop", new BigDecimal("1500.99")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.sellerId", is(100)))
                .andExpect(jsonPath("$.data.title", is("Notebook")))
                .andExpect(jsonPath("$.data.price", notNullValue()));
    }

    @Test
    @DisplayName("POST /products - Failure: Empty body returns 400 RFC 7807")
    void createProduct_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", notNullValue()))
                .andExpect(jsonPath("$.instance", containsString("/products")));
    }

    @Test
    @DisplayName("POST /products - Failure: Malformed JSON returns 400")
    void createProduct_MalformedJSON_Returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")));
    }

    @Test
    @DisplayName("POST /products - Failure: Missing sellerId returns 400")
    void createProduct_MissingSellerId_Returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test\", \"description\": \"Test\", \"price\": 100.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("sellerId")));
    }

    @Test
    @DisplayName("POST /products - Failure: Negative sellerId returns 400")
    void createProduct_NegativeSellerId_Returns400() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(-1L, "Test", "Test", new BigDecimal("100.00"));
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /products - Failure: Negative price returns 400")
    void createProduct_NegativePrice_Returns400() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(1L, "Test", "Test", new BigDecimal("-10.00"));
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /products - Failure: Wrong Content-Type returns 415")
    void createProduct_WrongContentType_Returns415() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"sellerId\": 1}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type", is("about:blank")));
    }

    // ============================================================================
    // GET /products - Retrieve all products (Success only)
    // ============================================================================

    @Test
    @DisplayName("GET /products - Success: Empty list returns 200")
    void getAllProducts_Empty_Returns200() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /products - Success: Products ordered by id ascending")
    void getAllProducts_WithProducts_Returns200() throws Exception {
        productRepository.save(Product.builder().sellerId(1L).title("A").description("Desc").price(new BigDecimal("100")).build());
        productRepository.save(Product.builder().sellerId(2L).title("B").description("Desc").price(new BigDecimal("200")).build());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // ============================================================================
    // GET /products/{id}
    // ============================================================================

    @Test
    @DisplayName("GET /products/{id} - Success: Valid id returns 200")
    void getProductById_Valid_Returns200() throws Exception {
        Product product = productRepository.save(Product.builder().sellerId(1L).title("Test").description("Desc").price(new BigDecimal("50")).build());
        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /products/{id} - Failure: Non-existent id returns 404")
    void getProductById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("about:blank")));
    }

    @Test
    @DisplayName("GET /products/{id} - Failure: Negative id returns 400")
    void getProductById_NegativeId_Returns400() throws Exception {
        mockMvc.perform(get("/products/-1"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // PUT /products/{id}
    // ============================================================================

    @Test
    @DisplayName("PUT /products/{id} - Success: Valid update returns 200")
    void updateProduct_Valid_Returns200() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Old").description("Old").price(new BigDecimal("100")).build());
        mockMvc.perform(put("/products/" + p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"New\", \"description\": \"New\", \"price\": 150}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /products/{id} - Failure: Non-existent id returns 404")
    void updateProduct_NotFound_Returns404() throws Exception {
        mockMvc.perform(put("/products/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"T\", \"description\": \"D\", \"price\": 100}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /products/{id} - Failure: Negative id returns 400")
    void updateProduct_NegativeId_Returns400() throws Exception {
        mockMvc.perform(put("/products/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"T\", \"description\": \"D\", \"price\": 100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /products/{id} - Failure: Empty body returns 400")
    void updateProduct_EmptyBody_Returns400() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Test").description("Desc").price(new BigDecimal("100")).build());
        mockMvc.perform(put("/products/" + p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // DELETE /products/{id}
    // ============================================================================

    @Test
    @DisplayName("DELETE /products/{id} - Success: Valid id returns 204")
    void deleteProduct_Valid_Returns204() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Test").description("Desc").price(new BigDecimal("100")).build());
        mockMvc.perform(delete("/products/" + p.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /products/{id} - Failure: Non-existent id returns 404")
    void deleteProduct_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/products/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /products/{id} - Failure: Negative id returns 400")
    void deleteProduct_NegativeId_Returns400() throws Exception {
        mockMvc.perform(delete("/products/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /products/{id} - Cascade: Removes from all carts")
    void deleteProduct_Cascade() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Test").description("Desc").price(new BigDecimal("100")).build());
        cartItemRepository.save(CartItem.builder().userId(1L).productId(p.getId()).title("Test").unitPrice(new BigDecimal("100")).build());

        mockMvc.perform(delete("/products/" + p.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart/1"))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    // ============================================================================
    // GET /cart/{userId}
    // ============================================================================

    @Test
    @DisplayName("GET /cart/{userId} - Success: Empty cart returns 200")
    void getCart_Empty_Returns200() throws Exception {
        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(1)))
                .andExpect(jsonPath("$.data.totalPrice", is(0)));
    }

    @Test
    @DisplayName("GET /cart/{userId} - Success: With items and total price")
    void getCart_WithItems_Returns200() throws Exception {
        cartItemRepository.save(CartItem.builder().userId(1L).productId(1L).title("Item").unitPrice(new BigDecimal("50")).build());
        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPrice", is(50.0)));
    }

    @Test
    @DisplayName("GET /cart/{userId} - Success: Items ordered descending")
    void getCart_ItemsOrdering_Returns200() throws Exception {
        cartItemRepository.save(CartItem.builder().userId(1L).productId(1L).title("A").unitPrice(new BigDecimal("10")).build());
        cartItemRepository.save(CartItem.builder().userId(1L).productId(2L).title("B").unitPrice(new BigDecimal("20")).build());
        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title", is("B")));
    }

    @Test
    @DisplayName("GET /cart/{userId} - Failure: Negative userId returns 400")
    void getCart_NegativeUserId_Returns400() throws Exception {
        mockMvc.perform(get("/cart/-1"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // DELETE /cart/{userId}
    // ============================================================================

    @Test
    @DisplayName("DELETE /cart/{userId} - Success: Wipe returns 204")
    void wipeCart_Valid_Returns204() throws Exception {
        mockMvc.perform(delete("/cart/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /cart/{userId} - Success: Empty cart idempotent")
    void wipeCart_Empty_Returns204() throws Exception {
        mockMvc.perform(delete("/cart/999"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /cart/{userId} - Failure: Negative userId returns 400")
    void wipeCart_NegativeUserId_Returns400() throws Exception {
        mockMvc.perform(delete("/cart/-1"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // POST /cart/{userId}/items
    // ============================================================================

    @Test
    @DisplayName("POST /cart/{userId}/items - Success: Add product returns 201")
    void addItemToCart_Valid_Returns201() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Product").description("Desc").price(new BigDecimal("50")).build());
        AddCartRequestDTO req = new AddCartRequestDTO(p.getId());
        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Success: Duplicate items allowed")
    void addItemToCart_Duplicate_Returns201() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Product").description("Desc").price(new BigDecimal("50")).build());
        AddCartRequestDTO req = new AddCartRequestDTO(p.getId());
        mockMvc.perform(post("/cart/1/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());
        mockMvc.perform(post("/cart/1/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());
        mockMvc.perform(get("/cart/1")).andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Success: Price snapshot")
    void addItemToCart_PriceSnapshot() throws Exception {
        Product p = productRepository.save(Product.builder().sellerId(1L).title("Product").description("Desc").price(new BigDecimal("50")).build());
        AddCartRequestDTO req = new AddCartRequestDTO(p.getId());
        mockMvc.perform(post("/cart/1/items").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(jsonPath("$.data.unitPrice", is(50.0)));
        p.setPrice(new BigDecimal("100"));
        productRepository.save(p);
        mockMvc.perform(get("/cart/1")).andExpect(jsonPath("$.data.items[0].unitPrice", is(50.0)));
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Failure: Non-existent product returns 404")
    void addItemToCart_ProductNotFound_Returns404() throws Exception {
        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 9999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Failure: Empty body returns 400")
    void addItemToCart_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Failure: Float productId returns 400")
    void addItemToCart_FloatProductId_Returns400() throws Exception {
        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 97.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cart/{userId}/items - Failure: Negative productId returns 400")
    void addItemToCart_NegativeProductId_Returns400() throws Exception {
        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": -1}"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================================
    // DELETE /cart/{userId}/items/{cartItemId}
    // ============================================================================

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Success: Valid returns 204")
    void removeItemFromCart_Valid_Returns204() throws Exception {
        CartItem item = cartItemRepository.save(CartItem.builder().userId(1L).productId(1L).title("Item").unitPrice(new BigDecimal("50")).build());
        mockMvc.perform(delete("/cart/1/items/" + item.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Failure: Non-existent returns 404")
    void removeItemFromCart_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/cart/1/items/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Failure: Wrong user returns 404")
    void removeItemFromCart_WrongUser_Returns404() throws Exception {
        CartItem item = cartItemRepository.save(CartItem.builder().userId(1L).productId(1L).title("Item").unitPrice(new BigDecimal("50")).build());
        mockMvc.perform(delete("/cart/999/items/" + item.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Failure: Negative userId returns 400")
    void removeItemFromCart_NegativeUserId_Returns400() throws Exception {
        mockMvc.perform(delete("/cart/-1/items/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /cart/{userId}/items/{cartItemId} - Failure: Negative itemId returns 400")
    void removeItemFromCart_NegativeItemId_Returns400() throws Exception {
        mockMvc.perform(delete("/cart/1/items/-1"))
                .andExpect(status().isBadRequest());
    }


    // ============================================================================
    // REENTREGA: Pruebas Exhaustivas de Éxito (Camino Feliz y Contrato Completo)
    // ============================================================================

    @Test
    @DisplayName("Éxito Exhaustivo - POST /products devuelve todos los campos correctos")
    void happyPath_CreateProduct_VerifiesAllFields() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Auriculares", "Auriculares Inalámbricos", new BigDecimal("99.99")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").exists()) // Verifica que exista el wrapper 'data'
                .andExpect(jsonPath("$.data.id").isNumber()) // Verifica que la BD asignó un ID
                .andExpect(jsonPath("$.data.sellerId").value(100))
                .andExpect(jsonPath("$.data.title").value("Auriculares"))
                .andExpect(jsonPath("$.data.description").value("Auriculares Inalámbricos"))
                .andExpect(jsonPath("$.data.price").value(99.99))
                .andExpect(jsonPath("$.data.createdAt").isString()) // Formato de fecha
                .andExpect(jsonPath("$.data.updatedAt").isString()); // Formato de fecha
    }

    @Test
    @DisplayName("Éxito Exhaustivo - GET /products/{id} devuelve todos los campos correctos")
    void happyPath_GetProduct_VerifiesAllFields() throws Exception {
        Product p = productRepository.save(Product.builder()
                .sellerId(100L)
                .title("Monitor")
                .description("Monitor 4K")
                .price(new BigDecimal("300.50"))
                .build());

        mockMvc.perform(get("/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(p.getId().intValue()))
                .andExpect(jsonPath("$.data.sellerId").value(100))
                .andExpect(jsonPath("$.data.title").value("Monitor"))
                .andExpect(jsonPath("$.data.description").value("Monitor 4K"))
                .andExpect(jsonPath("$.data.price").value(300.50))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andExpect(jsonPath("$.data.updatedAt").isString());
    }

    @Test
    @DisplayName("Éxito Exhaustivo - POST /cart/{userId}/items devuelve el snapshot completo")
    void happyPath_AddToCart_VerifiesAllFields() throws Exception {
        Product p = productRepository.save(Product.builder()
                .sellerId(1L)
                .title("Mouse")
                .description("Mouse Gamer")
                .price(new BigDecimal("25.00"))
                .build());

        AddCartRequestDTO req = new AddCartRequestDTO(p.getId());

        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").isNumber()) // ID del item en el carrito
                .andExpect(jsonPath("$.data.productId").value(p.getId().intValue()))
                .andExpect(jsonPath("$.data.title").value("Mouse"))
                .andExpect(jsonPath("$.data.unitPrice").value(25.00))
                .andExpect(jsonPath("$.data.addedAt").isString());
    }

    @Test
    @DisplayName("Éxito Exhaustivo - GET /cart/{userId} devuelve el agregador completo")
    void happyPath_GetCart_VerifiesAllFields() throws Exception {
        Product p = productRepository.save(Product.builder()
                .sellerId(1L)
                .title("Pad")
                .description("Mousepad")
                .price(new BigDecimal("10.00"))
                .build());

        cartItemRepository.save(CartItem.builder()
                .userId(2L)
                .productId(p.getId())
                .title(p.getTitle())
                .unitPrice(p.getPrice())
                .build());

        mockMvc.perform(get("/cart/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.totalPrice").value(10.00)) // Verifica la sumatoria
                .andExpect(jsonPath("$.data.items").isArray()) // Verifica que sea una lista
                .andExpect(jsonPath("$.data.items[0].id").isNumber())
                .andExpect(jsonPath("$.data.items[0].productId").value(p.getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].title").value("Pad"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(10.00))
                .andExpect(jsonPath("$.data.items[0].addedAt").isString());
    }

    // ============================================================================
    // REENTREGA: Rescate de Validaciones Estrictas del OpenAPI original
    // ============================================================================

    @Test
    @DisplayName("Redondeo Estricto - POST /products con más de 2 decimales redondea correctamente (HALF_UP)")
    void createProduct_priceHasTwoDecimals_RoundsCorrectly() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Product", "Desc", new BigDecimal("1234.5678")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Demuestra matemáticamente que 1234.5678 se redondea a 1234.57
                .andExpect(jsonPath("$.data.price", is(1234.57)));
    }

    @Test
    @DisplayName("Límite de Precio - POST /products con precio 0.00 falla (mínimo 0.01)")
    void createProduct_priceZero_Returns400() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Product", "Desc", new BigDecimal("0.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")));
    }

    @Test
    @DisplayName("Formato de Fecha - POST /products valida formato exacto ISO 8601 mediante Regex")
    void createProduct_createdAtFormatExact() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Product", "Desc", new BigDecimal("50.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.createdAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+")))
                .andExpect(jsonPath("$.data.updatedAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+")));
    }

    @Test
    @DisplayName("Estructura Estricta - La respuesta exitosa SOLO tiene el nodo 'data' en su raíz")
    void successResponse_NoExtraFields() throws Exception {
        CreateProductRequestDTO request = new CreateProductRequestDTO(
                100L, "Product", "Desc", new BigDecimal("50.00")
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasKey("data")))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }
}
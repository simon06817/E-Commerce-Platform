package com.example.project01.integration;

import com.example.project01.entity.Product;
import com.example.project01.entity.UserSeller;
import com.example.project01.service.ProductService;
import com.example.project01.service.UserSellerService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductSearchIntegrationTest extends IntegrationTestSupport {

    @Test
    void sellerOnlySeesOwnProducts() throws Exception {
        String sellerToken = login("SELLER", "seller01", "123456");
        mockMvc.perform(get("/api/seller/products")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode page = data(result);
                    assertEquals(3, page.path("total").asInt());
                });

        String otherSellerToken = login("SELLER", "seller02", "123456");
        mockMvc.perform(get("/api/seller/products")
                        .header("Authorization", "Bearer " + otherSellerToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode page = data(result);
                    assertEquals(0, page.path("total").asInt());
                });
    }

    @Autowired
    private UserSellerService userSellerService;

    @Autowired
    private ProductService productService;

    @Test
    void exactShopNameReturnsOnlyThatSellersProducts() throws Exception {
        JsonNode page = search("tEcH sToRe");

        assertEquals(3, page.path("total").asInt());
        assertSellerMatches(page, 1L, "Tech Store");
    }

    @Test
    void uniqueShopPrefixReturnsOnlyThatSellersProducts() throws Exception {
        JsonNode page = search("Tech");

        assertEquals(3, page.path("total").asInt());
        assertSellerMatches(page, 1L, "Tech Store");
    }

    @Test
    void productNameIsUsedWhenNoShopMatches() throws Exception {
        JsonNode page = search("iPhone");

        assertEquals(1, page.path("total").asInt());
        assertEquals("iPhone 15", page.path("records").get(0).path("name").asText());
        assertEquals("Tech Store", page.path("records").get(0).path("sellerName").asText());
    }

    @Test
    void ambiguousShopPrefixFallsBackToProductSearch() throws Exception {
        UserSeller extraSeller = new UserSeller();
        extraSeller.setUsername("searchshop");
        extraSeller.setPassword("123456");
        extraSeller.setShopName("Tech Annex");
        assertTrue(userSellerService.save(extraSeller));

        try {
            JsonNode page = search("Tech");
            assertEquals(0, page.path("total").asInt());
        } finally {
            userSellerService.removeById(extraSeller.getId());
        }
    }

    @Test
    void unknownKeywordReturnsEmptyPage() throws Exception {
        JsonNode page = search("definitely-no-shop-or-product");

        assertEquals(0, page.path("total").asInt());
        assertEquals(0, page.path("records").size());
    }

    @Test
    void combinedShopAliasAndProductKeywordReturnsExactProduct() throws Exception {
        UserSeller seller = new UserSeller();
        seller.setUsername("shop07seller");
        seller.setPassword("123456");
        seller.setShopName("Shop 07");
        assertTrue(userSellerService.save(seller));

        Product product = new Product();
        product.setSellerId(seller.getId());
        product.setName("遥控飞行无人机");
        product.setDescription("桌面遥控飞行玩具");
        product.setPrice(new java.math.BigDecimal("399.00"));
        product.setStock(20);
        product.setCategoryId(1L);
        product.setStatus(1);
        assertTrue(productService.save(product));

        try {
            JsonNode page = search("遥控飞行无人机", "shop7");
            assertEquals(1, page.path("total").asInt());
            assertEquals(product.getId(), page.path("records").get(0).path("id").asLong());
            assertEquals("Shop 07", page.path("records").get(0).path("sellerName").asText());
        } finally {
            productService.removeById(product.getId());
            userSellerService.removeById(seller.getId());
        }
    }

    @Test
    void productDetailIncludesSellerName() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("Tech Store", data(result).path("sellerName").asText());
    }

    @Test
    void recommendationsReturnSalesAndReviewMetricsByCategory() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/recommendations")
                        .param("categoryId", "1")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode recommendations = data(result);
        assertEquals(2, recommendations.size());
        for (JsonNode product : recommendations) {
            assertEquals(1, product.path("categoryId").asLong());
            assertTrue(product.has("salesQuantity"));
            assertTrue(product.has("reviewCount"));
            assertTrue(product.has("averageRating"));
        }
    }

    private JsonNode search(String keyword) throws Exception {
        return search(keyword, null);
    }

    private JsonNode search(String keyword, String shopName) throws Exception {
        var request = get("/api/products")
                .param("page", "1")
                .param("size", "20")
                .param("status", "1")
                .param("keyword", keyword);
        if (shopName != null) {
            request.param("shopName", shopName);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return data(result);
    }

    private void assertSellerMatches(JsonNode page, long sellerId, String sellerName) {
        for (JsonNode product : page.path("records")) {
            assertEquals(sellerId, product.path("sellerId").asLong());
            assertEquals(sellerName, product.path("sellerName").asText());
        }
    }
}

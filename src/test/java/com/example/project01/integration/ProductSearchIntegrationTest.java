package com.example.project01.integration;

import com.example.project01.entity.UserSeller;
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

    @Autowired
    private UserSellerService userSellerService;

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
    void productDetailIncludesSellerName() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("Tech Store", data(result).path("sellerName").asText());
    }

    private JsonNode search(String keyword) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "20")
                        .param("status", "1")
                        .param("keyword", keyword))
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

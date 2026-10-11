package com.ecommerce.platform.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductBloomFilterTest {

    @Test
    void beforeInitializationDoesNotRejectIds() {
        ProductBloomFilter filter = new ProductBloomFilter();

        assertFalse(filter.isReady());
        assertTrue(filter.mightContain(123L));
    }

    @Test
    void afterInitializationContainsInsertedIds() {
        ProductBloomFilter filter = new ProductBloomFilter();
        filter.add(1L);
        filter.markReady();

        assertTrue(filter.isReady());
        assertTrue(filter.mightContain(1L));
    }
}

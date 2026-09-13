package com.example.project01.cache;

import com.example.project01.entity.Product;
import com.example.project01.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Loads existing product ids into the bloom filter at application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBloomInitializer implements CommandLineRunner {

    private final ProductBloomFilter bloomFilter;
    private final ProductMapper productMapper;

    @Override
    public void run(String... args) {
        try {
            java.util.List<Long> ids = productMapper.selectList(null).stream()
                    .map(Product::getId)
                    .toList();
            bloomFilter.addAll(ids);
            log.info("Product bloom filter initialized with {} product ids", ids.size());
        } catch (Exception e) {
            log.warn("Product bloom filter initialization skipped: {}", e.getMessage());
        }
    }
}

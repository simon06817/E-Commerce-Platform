package com.ecommerce.platform.cache;

import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 商品布隆过滤器初始化器。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBloomInitializer implements CommandLineRunner {

    private final ProductBloomFilter bloomFilter;
    private final ProductMapper productMapper;

    /** 初始化商品布隆过滤器。 */
    @Override
    public void run(String... args) {
        java.util.List<Long> ids = productMapper.selectList(null).stream()
                .map(Product::getId)
                .toList();
        bloomFilter.addAll(ids);
        bloomFilter.markReady();
        log.info("Product bloom filter initialized with {} product ids", ids.size());
    }
}

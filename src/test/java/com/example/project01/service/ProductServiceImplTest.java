package com.example.project01.service;

import com.example.project01.cache.CacheSupport;
import com.example.project01.cache.ProductBloomFilter;
import com.example.project01.entity.Product;
import com.example.project01.mapper.ProductMapper;
import com.example.project01.observability.BusinessMetrics;
import com.example.project01.service.Impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductBloomFilter bloomFilter;

    @Mock
    private CacheSupport cacheSupport;

    @Mock
    private UserSellerService userSellerService;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "baseMapper", productMapper);
    }

    @Test
    void decreaseStockWithInsufficientStockFails() {
        Product product = new Product();
        product.setId(1L);
        product.setStock(1);
        when(productMapper.selectById(1L)).thenReturn(product);

        assertThrows(RuntimeException.class, () -> productService.decreaseStock(1L, 2));
        verify(productMapper, never()).update(any(), any());
    }

    @Test
    void decreaseStockSuccess() {
        Product product = new Product();
        product.setId(1L);
        product.setStock(10);
        when(productMapper.selectById(1L)).thenReturn(product);
        when(productMapper.update(any(), any())).thenReturn(1);

        productService.decreaseStock(1L, 3);

        verify(productMapper).update(any(), any());
    }
}

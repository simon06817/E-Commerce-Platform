package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.cache.CacheNames;
import com.example.project01.cache.CacheSupport;
import com.example.project01.cache.ProductBloomFilter;
import com.example.project01.common.BusinessException;
import com.example.project01.common.ProductStatusEnum;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.ProductRequest;
import com.example.project01.entity.Product;
import com.example.project01.mapper.ProductMapper;
import com.example.project01.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Product service with bloom-filter penetration protection, single-flight cache
 * rebuild and atomic stock updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductBloomFilter bloomFilter;
    private final CacheSupport cacheSupport;

    @Override
    public Page<Product> getProductPage(int current, int size, Long categoryId, String keyword, Integer status) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or()
                    .like(Product::getDescription, keyword));
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public Product getProductById(Long id) {
        // Bloom filter rejects ids that cannot exist, then read-through cache
        // protects the database from hot-key rebuilds.
        if (!bloomFilter.mightContain(id)) {
            return null;
        }
        return cacheSupport.getOrLoadWithNegativeCache(
                CacheNames.PRODUCT_DETAIL,
                CacheNames.PRODUCT_NULL,
                String.valueOf(id),
                () -> getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(Long sellerId, ProductRequest request) {
        Product product = new Product();
        product.setId(null);
        product.setSellerId(sellerId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setMainImage(request.getMainImage());
        product.setStatus(request.getStatus() == null
                ? ProductStatusEnum.ON_SALE.getCode() : request.getStatus());
        save(product);
        bloomFilter.add(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void updateProduct(Long sellerId, Long id, ProductRequest request) {
        Product existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        Product product = new Product();
        product.setId(id);
        product.setSellerId(sellerId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setMainImage(request.getMainImage());
        product.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void updateProductStatus(Long sellerId, Long id, Integer status) {
        Product existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void deleteProduct(Long sellerId, Long id) {
        Product existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        decreaseStock(product, quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#product.id")
    public void decreaseStock(Product product, Integer quantity) {
        if (product == null || product.getId() == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        // Early check gives a clear business error; the conditional UPDATE below
        // is still the concurrency-safe guard.
        if (product.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, product.getId())
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - {0}", quantity));
        if (!success) {
            if (getById(product.getId()) == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        int newStock = product.getStock() - quantity;
        refreshProductCacheAfterCommit(product.getId(), product, newStock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long productId, Integer quantity) {
        // Used when an unpaid order is cancelled or times out.
        Product product = getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("stock = stock + {0}", quantity));
        if (!success) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        refreshProductCacheAfterCommit(productId, product, product.getStock() + quantity);
    }

    private void refreshProductCacheAfterCommit(Long productId, Product product, int newStock) {
        // Cache refresh happens after commit so a rolled-back transaction never
        // leaves a misleading stock value in Redis.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    product.setStock(newStock);
                    product.setUpdateTime(LocalDateTime.now());
                    cacheSupport.put(CacheNames.PRODUCT_DETAIL, String.valueOf(productId), product);
                } catch (Exception e) {
                    log.warn("refresh product cache after stock change failed, id={}", productId, e);
                }
            }
        });
    }
}

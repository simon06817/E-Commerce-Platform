package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.ProductRequest;
import com.example.project01.entity.Product;
import com.example.project01.mapper.ProductMapper;
import com.example.project01.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

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
    @Cacheable(cacheNames = "productDetail", key = "#id")
    public Product getProductById(Long id) {
        return getById(id);
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
        product.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        save(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "productDetail", key = "#id")
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
    @CacheEvict(cacheNames = "productDetail", key = "#id")
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
    @CacheEvict(cacheNames = "productDetail", key = "#id")
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
    @CacheEvict(cacheNames = "productDetail", key = "#productId")
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (product.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity));
        if (!success) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
    }
}

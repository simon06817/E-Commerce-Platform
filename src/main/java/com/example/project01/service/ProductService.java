package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.dto.ProductRequest;
import com.example.project01.entity.Product;

/**
 * Product queries, seller-owned product management and stock changes.
 */
public interface ProductService extends IService<Product> {

    Page<Product> getProductPage(int current, int size, Long categoryId, String keyword, Integer status);

    Product getProductById(Long id);

    Product getProductDetail(Long id);

    void createProduct(Long sellerId, ProductRequest request);

    void updateProduct(Long sellerId, Long id, ProductRequest request);

    void updateProductStatus(Long sellerId, Long id, Integer status);

    void deleteProduct(Long sellerId, Long id);

    void decreaseStock(Long productId, Integer quantity);

    void decreaseStock(Product product, Integer quantity);

    void increaseStock(Long productId, Integer quantity);
}

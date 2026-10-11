package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.ProductCategory;

import java.util.List;

/**
 * Product category queries and administrator maintenance operations.
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    List<ProductCategory> listEnabledCategories();

    Page<ProductCategory> getCategoryPage(int current, int size, String name, Integer status);

    void addCategory(ProductCategory category);

    void updateCategory(ProductCategory category);

    void updateStatus(Long id, Integer status);

    void deleteCategory(Long id);
}

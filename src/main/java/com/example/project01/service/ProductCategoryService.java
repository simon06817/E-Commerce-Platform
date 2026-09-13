package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.ProductCategory;

import java.util.List;

/**
 * Product category queries and administrator maintenance operations.
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    List<ProductCategory> listEnabledCategories();

    Page<ProductCategory> getCategoryPage(int current, int size, String name);

    void addCategory(ProductCategory category);

    void updateCategory(ProductCategory category);

    void updateStatus(Long id, Integer status);

    void deleteCategory(Long id);
}

package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService extends IService<ProductCategory> {
    //分页查询分类列表
    Page<ProductCategory> getCategoryPage(int current, int size, String name);

    //获取所有启用的分类（用于下拉框）
    List<ProductCategory> getEnabledCategories();

    //新增分类
    void addCategory(ProductCategory category);

    //更新分类状态
    void updateStatus(Long id, Integer status);

}

package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.ProductCategory;
import com.example.project01.mapper.ProductCategoryMapper;
import com.example.project01.service.ProductCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {

    @Override
    public Page<ProductCategory> getCategoryPage(int current, int size, String name) {
        Page<ProductCategory> page= new Page<>(current, size);
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)){
            wrapper.like(ProductCategory::getName, name);
        }
        wrapper.orderByDesc(ProductCategory::getSortOrder);
        return this.page(page, wrapper);
    }

    @Override
    public List<ProductCategory> getEnabledCategories() {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getStatus, 1)
                .orderByDesc(ProductCategory::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCategory(ProductCategory category) {
        // 校验名称是否重复
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getName, category.getName());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("分类名称已存在");
        }
        this.save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<ProductCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductCategory::getId, id)
                .set(ProductCategory::getStatus, status);
        this.update(wrapper);
    }

}

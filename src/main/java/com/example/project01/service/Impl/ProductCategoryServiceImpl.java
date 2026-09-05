package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.cache.CacheNames;
import com.example.project01.cache.CacheSupport;
import com.example.project01.common.BusinessException;
import com.example.project01.entity.ProductCategory;
import com.example.project01.mapper.ProductCategoryMapper;
import com.example.project01.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    private final CacheSupport cacheSupport;

    @Override
    public List<ProductCategory> listEnabledCategories() {
        return cacheSupport.getOrLoad(
                CacheNames.CATEGORY_LIST,
                CacheNames.CATEGORY_LIST_KEY,
                () -> lambdaQuery()
                        .eq(ProductCategory::getStatus, 1)
                        .orderByAsc(ProductCategory::getSortOrder)
                        .list());
    }

    @Override
    public Page<ProductCategory> getCategoryPage(int current, int size, String name) {
        Page<ProductCategory> page = new Page<>(current, size);
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(ProductCategory::getName, name);
        }
        wrapper.orderByAsc(ProductCategory::getSortOrder);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    public void addCategory(ProductCategory category) {
        if (lambdaQuery().eq(ProductCategory::getName, category.getName()).count() > 0) {
            throw new BusinessException("category name already exists");
        }
        save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    public void updateCategory(ProductCategory category) {
        updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        update(new LambdaUpdateWrapper<ProductCategory>()
                .eq(ProductCategory::getId, id)
                .set(ProductCategory::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    public void deleteCategory(Long id) {
        removeById(id);
    }
}

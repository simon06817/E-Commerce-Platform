package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.cache.CacheNames;
import com.ecommerce.platform.cache.CacheSupport;
import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.common.CategoryStatusEnum;
import com.ecommerce.platform.entity.ProductCategory;
import com.ecommerce.platform.mapper.ProductCategoryMapper;
import com.ecommerce.platform.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** 商品分类服务实现。 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    private final CacheSupport cacheSupport;

    /** 查询启用中的商品分类。 */
    @Override
    public List<ProductCategory> listEnabledCategories() {
        return cacheSupport.getOrLoad(
                CacheNames.CATEGORY_LIST,
                CacheNames.CATEGORY_LIST_KEY,
                () -> lambdaQuery()
                .eq(ProductCategory::getStatus, CategoryStatusEnum.ENABLED.getCode())
                        .orderByAsc(ProductCategory::getSortOrder)
                        .list());
    }

    /** 分页查询商品分类。 */
    @Override
    public Page<ProductCategory> getCategoryPage(int current, int size, String name, Integer status) {
        Page<ProductCategory> page = new Page<>(current, size);
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(ProductCategory::getName, name);
        }
        if (status != null) {
            wrapper.eq(ProductCategory::getStatus, status);
        }
        wrapper.orderByAsc(ProductCategory::getSortOrder);
        return page(page, wrapper);
    }

    /** 新增商品分类。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true)
    public void addCategory(ProductCategory category) {
        if (lambdaQuery().eq(ProductCategory::getName, category.getName()).count() > 0) {
            throw new BusinessException("分类名称已存在");
        }
        save(category);
    }

    /** 修改商品分类。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true)
    public void updateCategory(ProductCategory category) {
        updateById(category);
    }

    /** 修改状态。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true)
    public void updateStatus(Long id, Integer status) {
        update(new LambdaUpdateWrapper<ProductCategory>()
                .eq(ProductCategory::getId, id)
                .set(ProductCategory::getStatus, status));
    }

    /** 删除商品分类。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.CATEGORY_LIST, allEntries = true)
    public void deleteCategory(Long id) {
        removeById(id);
    }
}

package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.ProductCategory;

import java.util.List;

/** 商品分类服务。 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /** 查询启用中的商品分类。 */
    List<ProductCategory> listEnabledCategories();

    /** 分页查询商品分类。 */
    Page<ProductCategory> getCategoryPage(int current, int size, String name, Integer status);

    /** 新增商品分类。 */
    void addCategory(ProductCategory category);

    /** 修改商品分类。 */
    void updateCategory(ProductCategory category);

    /** 修改状态。 */
    void updateStatus(Long id, Integer status);

    /** 删除商品分类。 */
    void deleteCategory(Long id);
}

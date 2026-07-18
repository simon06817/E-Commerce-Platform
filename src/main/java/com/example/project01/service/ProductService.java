package com.example.project01.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.Product;

public interface ProductService extends IService<Product> {
    // 分页查询商品列表
    Page<Product> getProductPage(int current, int size, String name, String categoryId);

    //上架或下架商品
    void updateStatus(Long id, Integer status);

    //下单减库存
    void decreaseStock(Long productId, Integer quantity);

}

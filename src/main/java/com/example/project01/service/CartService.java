package com.example.project01.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.Cart;

import java.util.List;

public interface CartService extends IService<Cart> {

    // 获取购物车列表
    List<Cart> getCartList(Long userId);

    // 添加商品到购物车(存在则增加数量)
    void addCart(Long userId, Long productId, Integer num);

    // 修改购物车商品数量
    void updateCartNum(Long cartId, Integer num);

    // 删除购物车商品
    void deleteCart(Long cartId);

    // 下单后清空购物车
    void clearCart(Long userId);

}

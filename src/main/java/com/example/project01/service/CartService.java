package com.example.project01.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.project01.entity.Cart;

import java.util.List;

public interface CartService extends IService<Cart> {

    List<Cart> getCartList(Long buyerId);

    void addCart(Long buyerId, Long productId, Integer num);

    void updateCartNum(Long buyerId, Long cartId, Integer num);

    void deleteCart(Long buyerId, Long cartId);

    void clearCart(Long buyerId);
}

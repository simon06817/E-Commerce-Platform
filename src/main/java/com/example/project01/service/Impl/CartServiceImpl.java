package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.common.BusinessException;
import com.example.project01.common.ResultCode;
import com.example.project01.entity.Cart;
import com.example.project01.entity.Product;
import com.example.project01.mapper.CartMapper;
import com.example.project01.service.CartService;
import com.example.project01.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final ProductService productService;

    @Override
    public List<Cart> getCartList(Long buyerId) {
        return lambdaQuery()
                .eq(Cart::getBuyerId, buyerId)
                .orderByDesc(Cart::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCart(Long buyerId, Long productId, Integer num) {
        Product product = productService.getProductById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        Cart existing = lambdaQuery()
                .eq(Cart::getBuyerId, buyerId)
                .eq(Cart::getProductId, productId)
                .one();
        if (existing != null) {
            existing.setNum(existing.getNum() + num);
            updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setBuyerId(buyerId);
            cart.setProductId(productId);
            cart.setNum(num);
            cart.setChecked(true);
            save(cart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCartNum(Long buyerId, Long cartId, Integer num) {
        Cart cart = getById(cartId);
        if (cart == null || !buyerId.equals(cart.getBuyerId())) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        cart.setNum(num);
        updateById(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCart(Long buyerId, Long cartId) {
        Cart cart = getById(cartId);
        if (cart == null || !buyerId.equals(cart.getBuyerId())) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        removeById(cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long buyerId) {
        remove(new LambdaQueryWrapper<Cart>().eq(Cart::getBuyerId, buyerId));
    }
}

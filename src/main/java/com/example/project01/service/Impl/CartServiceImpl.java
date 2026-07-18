package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.entity.Cart;
import com.example.project01.entity.Product;
import com.example.project01.mapper.CartMapper;
import com.example.project01.service.CartService;
import com.example.project01.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private ProductService productService;

    @Override
    public List<Cart> getCartList(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public void addCart(Long userId, Long productId, Integer num) {
        //商品是否存在
        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        //检查购物车是否有该商品
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId);
        Cart existCart = this.getOne(wrapper);
        if (existCart != null) {
            existCart.setNum(existCart.getNum() + num);
            this.updateById(existCart);
        }else{
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setNum(num);
            cart.setChecked(true);
            this.save(cart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCartNum(Long cartId, Integer num) {
        Cart cart = this.getById(cartId);
        if (cart == null) {
            throw new RuntimeException("购物车记录不存在");
        }
        cart.setNum(num);
        this.updateById(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCart(Long cartId) {
        this.removeById(cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        LambdaUpdateWrapper<Cart> wrapper=new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        this.remove(wrapper);
    }
}

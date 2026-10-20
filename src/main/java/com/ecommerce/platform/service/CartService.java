package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.entity.Cart;

import java.util.Collection;
import java.util.List;

/** 购物车服务。 */
public interface CartService extends IService<Cart> {

    /** 查询购物车列表。 */
    List<Cart> getCartList(Long buyerId);

    /** 添加商品到购物车。 */
    void addCart(Long buyerId, Long productId, Integer num);

    /** 修改购物车商品数量。 */
    void updateCartNum(Long buyerId, Long cartId, Integer num);

    /** 删除购物车项。 */
    void deleteCart(Long buyerId, Long cartId);

    /** 清空购物车。 */
    void clearCart(Long buyerId);

    /** 删除结算成功的购物车项。 */
    void removeSelected(Long buyerId, Collection<Long> cartIds);
}

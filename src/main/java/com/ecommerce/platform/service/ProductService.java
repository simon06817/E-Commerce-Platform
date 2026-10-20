package com.ecommerce.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.vo.ProductRecommendationVO;

import java.util.List;

/** 商品服务。 */
public interface ProductService extends IService<Product> {

    /** 分页查询商品。 */
    Page<Product> getProductPage(int current, int size, Long categoryId, String keyword,
                                 String shopName, Integer status);

    /** 分页查询卖家商品。 */
    Page<Product> getSellerProductPage(Long sellerId, int current, int size,
                                       Long categoryId, String keyword, Integer status);

    /** 根据主键查询商品。 */
    Product getProductById(Long id);

    /** 查询商品详情。 */
    Product getProductDetail(Long id);

    /** 查询商品推荐列表。 */
    List<ProductRecommendationVO> getRecommendations(Long categoryId, int limit);

    /** 创建商品。 */
    void createProduct(Long sellerId, ProductRequest request);

    /** 修改商品。 */
    void updateProduct(Long sellerId, Long id, ProductRequest request);

    /** 修改商品上下架状态。 */
    void updateProductStatus(Long sellerId, Long id, Integer status);

    /** 删除商品。 */
    void deleteProduct(Long sellerId, Long id);

    /** 扣减商品库存。 */
    void decreaseStock(Long productId, Integer quantity);

    /** 扣减商品库存。 */
    void decreaseStock(Product product, Integer quantity);

    /** 增加商品库存。 */
    void increaseStock(Long productId, Integer quantity);
}

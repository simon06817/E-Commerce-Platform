package com.ecommerce.platform.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.platform.cache.CacheNames;
import com.ecommerce.platform.cache.CacheSupport;
import com.ecommerce.platform.cache.ProductBloomFilter;
import com.ecommerce.platform.common.BusinessException;
import com.ecommerce.platform.common.ProductStatusEnum;
import com.ecommerce.platform.common.ResultCode;
import com.ecommerce.platform.dto.ProductRequest;
import com.ecommerce.platform.entity.Product;
import com.ecommerce.platform.entity.UserSeller;
import com.ecommerce.platform.mapper.ProductMapper;
import com.ecommerce.platform.service.ProductService;
import com.ecommerce.platform.service.UserSellerService;
import com.ecommerce.platform.vo.ProductRecommendationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 商品服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("^(.*?)(\\d+)$");

    private final ProductBloomFilter bloomFilter;
    private final CacheSupport cacheSupport;
    private final UserSellerService userSellerService;

    /** 分页查询商品。 */
    @Override
    public Page<Product> getProductPage(int current, int size, Long categoryId, String keyword,
                                        String shopName, Integer status) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(shopName)) {
            Long sellerId = resolveSellerId(shopName.trim());
            if (sellerId == null) {
                return page;
            }
            wrapper.eq(Product::getSellerId, sellerId);
        }
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            Long sellerId = StringUtils.hasText(shopName)
                    ? null : resolveSellerId(normalizedKeyword);
            if (sellerId != null) {
                wrapper.eq(Product::getSellerId, sellerId);
            } else {
                wrapper.and(w -> w.like(Product::getName, normalizedKeyword)
                        .or()
                        .like(Product::getDescription, normalizedKeyword));
            }
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = page(page, wrapper);
        enrichSellerNames(result.getRecords());
        return result;
    }

    /** 根据主键查询商品。 */
    @Override
    public Product getProductById(Long id) {
        // 布隆过滤器先排除不存在的 ID，再走缓存。
        // 避免热点键失效后大量请求直接查库。
        if (!bloomFilter.mightContain(id)) {
            return null;
        }
        return cacheSupport.getOrLoadWithNegativeCache(
                CacheNames.PRODUCT_DETAIL,
                CacheNames.PRODUCT_NULL,
                String.valueOf(id),
                () -> getById(id));
    }

    /** 分页查询卖家商品。 */
    @Override
    public Page<Product> getSellerProductPage(Long sellerId, int current, int size,
                                              Long categoryId, String keyword, Integer status) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerId);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(Product::getName, normalizedKeyword)
                    .or()
                    .like(Product::getDescription, normalizedKeyword));
        }
        wrapper.orderByDesc(Product::getUpdateTime);
        Page<Product> result = page(page, wrapper);
        enrichSellerNames(result.getRecords());
        return result;
    }

    /** 查询商品详情。 */
    @Override
    public Product getProductDetail(Long id) {
        Product product = getProductById(id);
        if (product != null) {
            enrichSellerNames(List.of(product));
        }
        return product;
    }

    /** 查询商品推荐列表。 */
    @Override
    public List<ProductRecommendationVO> getRecommendations(Long categoryId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return baseMapper.selectRecommendations(categoryId, safeLimit);
    }

    /** 根据关键词解析唯一卖家。 */
    private Long resolveSellerId(String keyword) {
        // 完整店铺名优先精确匹配，避免混入其他卖家商品。
        // 精确匹配结果唯一且稳定。
        List<UserSeller> exactMatches = userSellerService.lambdaQuery()
                .select(UserSeller::getId)
                .apply("LOWER(shop_name) = LOWER({0})", keyword)
                .list();
        if (exactMatches.size() == 1) {
            return exactMatches.get(0).getId();
        }
        if (exactMatches.size() > 1) {
            return null;
        }

        // 前缀匹配仅在唯一命中一个卖家时生效。
        List<UserSeller> prefixMatches = userSellerService.lambdaQuery()
                .select(UserSeller::getId)
                .apply("LOWER(shop_name) LIKE CONCAT(LOWER({0}), '%')", keyword)
                .list();
        if (prefixMatches.size() == 1) {
            return prefixMatches.get(0).getId();
        }
        if (prefixMatches.size() > 1) {
            return null;
        }

        // 用户常把 Shop 07 写成 shop7，先走索引精确或前缀匹配。
        // 未命中时再使用忽略空格、分隔符和前导零的兼容匹配。
        // 兼容 Shop 07 与 shop7 等写法。
        String normalizedKeyword = normalizeShopName(keyword);
        List<Long> normalizedMatches = userSellerService.lambdaQuery()
                .select(UserSeller::getId, UserSeller::getShopName)
                .list()
                .stream()
                .filter(seller -> normalizedKeyword.equals(
                        normalizeShopName(seller.getShopName())))
                .map(UserSeller::getId)
                .toList();
        return normalizedMatches.size() == 1 ? normalizedMatches.get(0) : null;
    }

    /** 规范化店铺名称。 */
    private String normalizeShopName(String shopName) {
        if (!StringUtils.hasText(shopName)) {
            return "";
        }
        String compact = shopName.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\-_]+", "");
        Matcher matcher = TRAILING_DIGITS.matcher(compact);
        if (!matcher.matches()) {
            return compact;
        }
        String number = matcher.group(2).replaceFirst("^0+(?!$)", "");
        return matcher.group(1) + number;
    }

    /** 补充商品所属店铺名称。 */
    private void enrichSellerNames(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        Set<Long> sellerIds = products.stream()
                .map(Product::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sellerIds.isEmpty()) {
            return;
        }
        Map<Long, UserSeller> sellersById = userSellerService.lambdaQuery()
                .in(UserSeller::getId, sellerIds)
                .list()
                .stream()
                .collect(Collectors.toMap(UserSeller::getId, Function.identity()));
        for (Product product : products) {
            UserSeller seller = sellersById.get(product.getSellerId());
            if (seller != null) {
                product.setSellerName(StringUtils.hasText(seller.getShopName())
                        ? seller.getShopName() : seller.getUsername());
            }
        }
    }

    /** 创建商品。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(Long sellerId, ProductRequest request) {
        Product product = new Product();
        product.setId(null);
        product.setSellerId(sellerId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setMainImage(request.getMainImage());
        product.setStatus(request.getStatus() == null
                ? ProductStatusEnum.ON_SALE.getCode() : request.getStatus());
        save(product);
        bloomFilter.add(product.getId());
    }

    /** 修改商品。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void updateProduct(Long sellerId, Long id, ProductRequest request) {
        Product existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        Product product = new Product();
        product.setId(id);
        product.setSellerId(sellerId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setMainImage(request.getMainImage());
        product.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        updateById(product);
    }

    /** 修改商品上下架状态。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void updateProductStatus(Long sellerId, Long id, Integer status) {
        Product existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    /** 删除商品。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void deleteProduct(Long sellerId, Long id) {
        Product existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_OWNED);
        }
        removeById(id);
    }

    /** 扣减商品库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        decreaseStock(product, quantity);
    }

    /** 扣减商品库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#product.id")
    public void decreaseStock(Product product, Integer quantity) {
        if (product == null || product.getId() == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        // 前置检查用于返回明确业务错误，最终并发安全仍由条件更新保证。
        // 条件更新仍是并发安全的最终保障。
        if (product.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, product.getId())
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - {0}", quantity));
        if (!success) {
            if (getById(product.getId()) == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        int newStock = product.getStock() - quantity;
        refreshProductCacheAfterCommit(product.getId(), product, newStock);
    }

    /** 增加商品库存。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long productId, Integer quantity) {
        // 待付款订单取消或超时关单时恢复库存。
        Product product = getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("stock = stock + {0}", quantity));
        if (!success) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        refreshProductCacheAfterCommit(productId, product, product.getStock() + quantity);
    }

    /** 事务提交后刷新商品缓存。 */
    private void refreshProductCacheAfterCommit(Long productId, Product product, int newStock) {
        // 缓存刷新放在事务提交后，避免回滚事务写入错误库存。
        // 防止 Redis 留下与数据库不一致的库存。
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 事务提交后刷新商品缓存。 */
            @Override
            public void afterCommit() {
                try {
                    product.setStock(newStock);
                    product.setUpdateTime(LocalDateTime.now());
                    cacheSupport.put(CacheNames.PRODUCT_DETAIL, String.valueOf(productId), product);
                } catch (Exception e) {
                    log.warn("refresh product cache after stock change failed, id={}", productId, e);
                }
            }
        });
    }
}

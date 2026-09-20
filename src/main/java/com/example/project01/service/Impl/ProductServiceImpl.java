package com.example.project01.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.project01.cache.CacheNames;
import com.example.project01.cache.CacheSupport;
import com.example.project01.cache.ProductBloomFilter;
import com.example.project01.common.BusinessException;
import com.example.project01.common.ProductStatusEnum;
import com.example.project01.common.ResultCode;
import com.example.project01.dto.ProductRequest;
import com.example.project01.entity.Product;
import com.example.project01.entity.UserSeller;
import com.example.project01.mapper.ProductMapper;
import com.example.project01.observability.BusinessMetrics;
import com.example.project01.service.ProductService;
import com.example.project01.service.UserSellerService;
import com.example.project01.vo.ProductRecommendationVO;
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

/**
 * Product service with bloom-filter penetration protection, single-flight cache
 * rebuild and atomic stock updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final Pattern TRAILING_DIGITS = Pattern.compile("^(.*?)(\\d+)$");

    private final ProductBloomFilter bloomFilter;
    private final CacheSupport cacheSupport;
    private final UserSellerService userSellerService;
    private final BusinessMetrics businessMetrics;

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

    @Override
    public Product getProductById(Long id) {
        // Bloom filter rejects ids that cannot exist, then read-through cache
        // protects the database from hot-key rebuilds.
        if (!bloomFilter.mightContain(id)) {
            return null;
        }
        return cacheSupport.getOrLoadWithNegativeCache(
                CacheNames.PRODUCT_DETAIL,
                CacheNames.PRODUCT_NULL,
                String.valueOf(id),
                () -> getById(id));
    }

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

    @Override
    public Product getProductDetail(Long id) {
        Product product = getProductById(id);
        if (product != null) {
            enrichSellerNames(List.of(product));
        }
        return product;
    }

    @Override
    public List<ProductRecommendationVO> getRecommendations(Long categoryId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return baseMapper.selectRecommendations(categoryId, safeLimit);
    }

    private Long resolveSellerId(String keyword) {
        // Exact match takes precedence so a complete shop name always behaves
        // deterministically and never mixes products from multiple sellers.
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

        // Prefix matching is allowed only when it resolves to one seller.
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

        // Users commonly type Shop 07 as shop7. Keep the indexed exact/prefix
        // checks above as the fast path, then fall back to a normalized match
        // that ignores spaces, separators and leading zeros in numeric suffixes.
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        decreaseStock(product, quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#product.id")
    public void decreaseStock(Product product, Integer quantity) {
        if (product == null || product.getId() == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        // Early check gives a clear business error; the conditional UPDATE below
        // is still the concurrency-safe guard.
        if (product.getStock() < quantity) {
            businessMetrics.recordStockChange("decrease", false);
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, product.getId())
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - {0}", quantity));
        if (!success) {
            businessMetrics.recordStockChange("decrease", false);
            if (getById(product.getId()) == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT);
        }
        businessMetrics.recordStockChange("decrease", true);
        int newStock = product.getStock() - quantity;
        refreshProductCacheAfterCommit(product.getId(), product, newStock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long productId, Integer quantity) {
        // Used when an unpaid order is cancelled or times out.
        Product product = getById(productId);
        if (product == null) {
            businessMetrics.recordStockChange("increase", false);
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        boolean success = update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("stock = stock + {0}", quantity));
        if (!success) {
            businessMetrics.recordStockChange("increase", false);
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        businessMetrics.recordStockChange("increase", true);
        refreshProductCacheAfterCommit(productId, product, product.getStock() + quantity);
    }

    private void refreshProductCacheAfterCommit(Long productId, Product product, int newStock) {
        // Cache refresh happens after commit so a rolled-back transaction never
        // leaves a misleading stock value in Redis.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
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

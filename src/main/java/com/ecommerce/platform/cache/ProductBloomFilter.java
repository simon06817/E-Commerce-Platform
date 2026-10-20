package com.ecommerce.platform.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.springframework.stereotype.Component;

import java.util.Collection;

/** 商品布隆过滤器。 */
@Component
public class ProductBloomFilter {

    private static final int EXPECTED_INSERTIONS = 100_000;
    private static final double FPP = 0.01;

    private final Funnel<Long> productIdFunnel = (Long from, PrimitiveSink into) -> into.putLong(from);
    private final BloomFilter<Long> filter =
            BloomFilter.create(productIdFunnel, EXPECTED_INSERTIONS, FPP);
    private volatile boolean ready;

    /** 将商品 ID 加入布隆过滤器。 */
    public void add(Long id) {
        if (id != null) {
            filter.put(id);
        }
    }

    /** 批量加入数据。 */
    public void addAll(Collection<Long> ids) {
        if (ids != null) {
            ids.forEach(this::add);
        }
    }

    /** 标记布隆过滤器初始化完成。 */
    public void markReady() {
        ready = true;
    }

    /** 判断布隆过滤器是否初始化完成。 */
    public boolean isReady() {
        return ready;
    }

    /** 判断商品 ID 是否可能存在。 */
    public boolean mightContain(Long id) {
        if (id == null) {
            return false;
        }
        // 初始化完成前放行请求，避免误拦截。
        return !ready || filter.mightContain(id);
    }
}

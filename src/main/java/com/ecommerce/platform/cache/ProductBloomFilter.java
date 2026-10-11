package com.ecommerce.platform.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * In-memory Guava bloom filter used to reject queries for product ids that do
 * not exist, reducing cache penetration to the database.
 */
@Component
public class ProductBloomFilter {

    private static final int EXPECTED_INSERTIONS = 100_000;
    private static final double FPP = 0.01;

    private final Funnel<Long> productIdFunnel = (Long from, PrimitiveSink into) -> into.putLong(from);
    private final BloomFilter<Long> filter =
            BloomFilter.create(productIdFunnel, EXPECTED_INSERTIONS, FPP);
    private volatile boolean ready;

    public void add(Long id) {
        if (id != null) {
            filter.put(id);
        }
    }

    public void addAll(Collection<Long> ids) {
        if (ids != null) {
            ids.forEach(this::add);
        }
    }

    public void markReady() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean mightContain(Long id) {
        if (id == null) {
            return false;
        }
        // Before initialization completes, allow the request to reach the cache and database.
        return !ready || filter.mightContain(id);
    }
}

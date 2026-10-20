package com.ecommerce.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 本地缓存配置。 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
public class LocalCacheConfig {

    /** 创建缓存管理器。 */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}

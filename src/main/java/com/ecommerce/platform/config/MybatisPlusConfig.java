package com.ecommerce.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置。 */
@Configuration
public class MybatisPlusConfig {

    /** 注入分页拦截器。 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建分页插件。
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 创建分页内部拦截器。
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();

        // 设置数据库类型。
        paginationInnerInterceptor.setDbType(DbType.MYSQL);

        // 超出总页数时返回空数据。
        paginationInnerInterceptor.setOverflow(false);

        // 限制单页最大查询条数。
        // 单页最多查询 100 条。
        paginationInnerInterceptor.setMaxLimit(100L);

        // 将分页插件加入拦截器链。
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}


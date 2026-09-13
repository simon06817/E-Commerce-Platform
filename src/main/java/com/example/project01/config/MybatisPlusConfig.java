package com.example.project01.config; // 请替换为你的实际包路径

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类 (兼容 JDK 17 & MP 3.5.15)
 * 用于启用内部分页插件功能
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注入分页拦截器容器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 1. 创建核心插件容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 2. 实例化分页内部拦截器 (PaginationInnerInterceptor)
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();

        // 3. 设置数据库类型 (根据你的实际数据库类型修改, 如 MYSQL, POSTGRE_SQL, ORACLE)
        paginationInnerInterceptor.setDbType(DbType.MYSQL);

        // 4. 可选: 设置溢出处理 (如: overflow=false 超出总页数返回空数据而不是最后一页)
        paginationInnerInterceptor.setOverflow(false);

        // 5. 可选: 单页最大查询限制 (限制攻击, 默认无限制)
        // Hard cap every page query at 100 rows to avoid oversized requests.
        paginationInnerInterceptor.setMaxLimit(100L);

        // 6. 将分页插件添加到拦截器链中 (建议排在第一位)
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}


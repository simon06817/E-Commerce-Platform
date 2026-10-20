package com.ecommerce.platform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/** Web 配置。 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    /** 配置跨域访问规则。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /** 配置上传文件的访问路径。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = uploadProperties.getDir();
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator;
        }
        String prefix = uploadProperties.getUrlPrefix();
        registry.addResourceHandler(prefix + "**").addResourceLocations("file:" + dir);
    }
}

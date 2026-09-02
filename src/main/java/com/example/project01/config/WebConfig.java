package com.example.project01.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

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

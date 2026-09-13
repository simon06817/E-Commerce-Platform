package com.example.project01.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds Aliyun DashScope settings (API key and model name) from configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.ai")
public class AIConfig {

    private String apiKey;

    private String model = "qwen-turbo";
}


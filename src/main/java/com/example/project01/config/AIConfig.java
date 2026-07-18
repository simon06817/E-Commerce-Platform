package com.example.project01.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.ai")
public class AIConfig {

    private String apiKey;

    private String model = "qwen-turbo";
}


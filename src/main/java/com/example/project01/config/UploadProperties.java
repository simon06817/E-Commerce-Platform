package com.example.project01.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String dir;

    private String urlPrefix;
}

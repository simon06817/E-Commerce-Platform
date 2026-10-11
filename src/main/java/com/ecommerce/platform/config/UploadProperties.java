package com.ecommerce.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for local file upload storage.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String dir;

    private String urlPrefix;
}

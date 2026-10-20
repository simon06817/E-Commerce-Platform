package com.ecommerce.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 上传配置属性。 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String dir;

    private String urlPrefix;
}

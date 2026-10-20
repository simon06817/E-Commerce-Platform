package com.ecommerce.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ecommerce.platform.mapper")
@EnableScheduling
/** 电商平台启动类。 */
public class EcommercePlatformApplication {

    /** 启动应用。 */
    public static void main(String[] args) {
        SpringApplication.run(EcommercePlatformApplication.class, args);
    }

}

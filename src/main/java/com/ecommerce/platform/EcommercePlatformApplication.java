package com.ecommerce.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ecommerce.platform.mapper")
@EnableScheduling
/**
 * Application bootstrap: enables component scanning, MyBatis mapper scanning
 * and scheduled tasks such as order timeout handling.
 */
public class EcommercePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommercePlatformApplication.class, args);
    }

}

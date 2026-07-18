package com.example.project01.MapperTest;


import com.example.project01.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    void testDatabaseConnection() {
        long count = productMapper.selectCount(null);
        System.out.println("数据库连接成功！商品表记录数: " + count);
    }
}

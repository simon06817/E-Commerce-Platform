package com.example.project01.MapperTest;

import com.example.project01.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper usermapper;

    @Test
    void testDatabaseConnection() {
        long count = usermapper.selectCount(null);
        System.out.println("数据库连接成功！用户表记录数: " + count);
    }
}

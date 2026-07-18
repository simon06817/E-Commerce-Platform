package com.example.project01;

import com.example.project01.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Project01ApplicationTests {

    @Test
    void contextLoads() {
        User user = new User();
        user.setUsername("admin");
    }

}

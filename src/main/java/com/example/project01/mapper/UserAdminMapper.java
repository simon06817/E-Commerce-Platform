package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.UserAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for administrator accounts.
 */
@Mapper
public interface UserAdminMapper extends BaseMapper<UserAdmin> {
}

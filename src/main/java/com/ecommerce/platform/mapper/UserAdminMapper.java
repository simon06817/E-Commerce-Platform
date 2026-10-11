package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.UserAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for administrator accounts.
 */
@Mapper
public interface UserAdminMapper extends BaseMapper<UserAdmin> {
}

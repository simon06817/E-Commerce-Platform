package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.UserAdmin;
import org.apache.ibatis.annotations.Mapper;

/** 管理员账号数据访问接口。 */
@Mapper
public interface UserAdminMapper extends BaseMapper<UserAdmin> {
}

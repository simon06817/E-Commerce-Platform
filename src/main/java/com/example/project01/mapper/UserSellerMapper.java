package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.UserSeller;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for seller accounts.
 */
@Mapper
public interface UserSellerMapper extends BaseMapper<UserSeller> {
}

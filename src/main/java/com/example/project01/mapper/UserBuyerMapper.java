package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.UserBuyer;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for buyer accounts.
 */
@Mapper
public interface UserBuyerMapper extends BaseMapper<UserBuyer> {
}

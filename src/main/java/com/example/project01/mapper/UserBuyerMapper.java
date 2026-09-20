package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.UserBuyer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis-Plus mapper for buyer accounts.
 */
@Mapper
public interface UserBuyerMapper extends BaseMapper<UserBuyer> {

    @Select("""
            SELECT id, username, password, nickname, phone, email, address, deleted
            FROM user_buyer
            WHERE username = #{username}
            LIMIT 1
            """)
    UserBuyer selectAnyByUsername(@Param("username") String username);

    @Update("""
            UPDATE user_buyer
            SET password = #{buyer.password},
                nickname = #{buyer.nickname},
                phone = #{buyer.phone},
                email = #{buyer.email},
                address = #{buyer.address},
                deleted = 0,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{buyer.id}
            """)
    int restoreDeleted(@Param("buyer") UserBuyer buyer);
}

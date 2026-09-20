package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.UserSeller;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis-Plus mapper for seller accounts.
 */
@Mapper
public interface UserSellerMapper extends BaseMapper<UserSeller> {

    @Select("""
            SELECT id, username, password, shop_name, phone, email, deleted
            FROM user_seller
            WHERE username = #{username}
            LIMIT 1
            """)
    UserSeller selectAnyByUsername(@Param("username") String username);

    @Update("""
            UPDATE user_seller
            SET password = #{seller.password},
                shop_name = #{seller.shopName},
                phone = #{seller.phone},
                email = #{seller.email},
                deleted = 0,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{seller.id}
            """)
    int restoreDeleted(@Param("seller") UserSeller seller);
}

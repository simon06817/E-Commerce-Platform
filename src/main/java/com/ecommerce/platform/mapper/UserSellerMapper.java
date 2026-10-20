package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.UserSeller;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 卖家账号数据访问接口。 */
@Mapper
public interface UserSellerMapper extends BaseMapper<UserSeller> {

    /** 查询任意状态的账号记录。 */
    @Select("""
            SELECT id, username, password, shop_name, phone, email, deleted
            FROM user_seller
            WHERE username = #{username}
            LIMIT 1
            """)
    UserSeller selectAnyByUsername(@Param("username") String username);

    /** 恢复逻辑删除的账号。 */
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

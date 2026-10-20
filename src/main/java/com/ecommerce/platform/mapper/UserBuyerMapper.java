package com.ecommerce.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.platform.entity.UserBuyer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 买家账号数据访问接口。 */
@Mapper
public interface UserBuyerMapper extends BaseMapper<UserBuyer> {

    /** 查询任意状态的账号记录。 */
    @Select("""
            SELECT id, username, password, nickname, phone, email, address, deleted
            FROM user_buyer
            WHERE username = #{username}
            LIMIT 1
            """)
    UserBuyer selectAnyByUsername(@Param("username") String username);

    /** 恢复逻辑删除的账号。 */
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

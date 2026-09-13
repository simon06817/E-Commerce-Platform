package com.example.project01.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.project01.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for local outbox messages.
 */
@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {
}

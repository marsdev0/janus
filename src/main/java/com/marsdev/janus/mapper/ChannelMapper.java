package com.marsdev.janus.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marsdev.janus.entity.Channel;

import java.util.Arrays;
import java.util.List;

/**
 * @author geyan
 * @date 2026/7/31
 */
public interface ChannelMapper extends BaseMapper<Channel> {

    default List<Channel> findEnableByModel(String model) {
        List<Channel> all = selectList(new QueryWrapper<>());
        // 不能用contains(model)，需要改成精确匹配
        return all.stream().filter(ch -> ch.getStatus() == 1
                        && Arrays.stream(ch.getModels().split(",")).map(String::trim).anyMatch(model::equals))
                .toList();
    }
}

/*
 * Copyright (c) 2026 marsdev0
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package com.marsdev.janus.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marsdev.janus.entity.Token;

/**
 * @author geyan
 * @date 2026/8/2
 */
public interface TokenMapper extends BaseMapper<Token> {

    int ENABLE = 1;
    int DISABLE = 0;

    default Token findByKeyHash(String keyHash) {
        return selectOne(new LambdaQueryWrapper<Token>()
                .eq(Token::getKeyHash, keyHash));
    }
}

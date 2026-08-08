package com.marsdev.janus.common.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author geyan
 * @date 2026/8/8
 */
public class ScriptUtil {

    /**
     * 加载脚本
     */
    public static DefaultRedisScript<Long> load(String path) {
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        s.setResultType(Long.class);
        return s;
    }
}

package com.wobushi041.codemate.service;

import com.wobushi041.codemate.service.impl.RecommendCacheServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 推荐缓存服务实现单元测试
 *
 * @author wobushi041
 */
class RecommendCacheServiceImplTest {

    /**
     * 测试构建推荐缓存 Redis 键名包含用户 id、页码与分页大小
     */
    // 场景：测试根据用户 id、页码与每页条数拼接标准 Redis 缓存键名
    @Test
    void buildRecommendCacheKey_shouldIncludeUserIdPageNumAndPageSize() {
        // 1. 准备测试数据
        RecommendCacheServiceImpl service = new RecommendCacheServiceImpl();

        // 2. 调用待测方法
        String redisKey = service.buildRecommendCacheKey(1L, 2L, 10L);

        // 3. 断言结果
        assertEquals("user:recommend:1:2:10", redisKey);
    }

}

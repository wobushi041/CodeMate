package com.wobushi041.codemate.model.dto;

import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐缓存快照数据对象
 *
 * @author wobushi041
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCacheSnapshot {

    /**
     * Redis 缓存键
     */
    private String redisKey;

    /**
     * 推荐缓存命中状态
     */
    private RecommendCacheStatus status;

    /**
     * 推荐缓存值对象
     */
    private RecommendCacheValue cacheValue;

}

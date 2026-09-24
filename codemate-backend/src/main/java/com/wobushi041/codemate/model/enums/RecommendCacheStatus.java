package com.wobushi041.codemate.model.enums;

/**
 * 推荐缓存状态枚举
 *
 * @author wobushi041
 */
public enum RecommendCacheStatus {

    /**
     * 缓存未命中
     */
    ABSENT,

    /**
     * 触发异步预刷新
     */
    REFRESH_AHEAD,

    /**
     * 缓存逻辑过期
     */
    LOGIC_EXPIRED,

    /**
     * 缓存有效
     */
    VALID

}

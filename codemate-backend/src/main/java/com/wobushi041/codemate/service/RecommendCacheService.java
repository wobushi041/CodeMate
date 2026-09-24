package com.wobushi041.codemate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;

/**
 * 推荐用户缓存服务
 *
 * @author wobushi041
 */
public interface RecommendCacheService {

    /**
     * 分页获取主页推荐用户列表
     *
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @param userId   当前用户 id
     * @return 推荐用户分页列表
     */
    Page<User> getRecommendUsers(long pageNum, long pageSize, long userId);

    /**
     * 刷新并获取指定用户的推荐用户分页列表
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 刷新后的推荐用户分页列表
     */
    Page<User> refreshRecommendUsers(long userId, long pageNum, long pageSize);

    /**
     * 刷新指定用户的推荐缓存并返回最新缓存快照
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 刷新后的推荐缓存快照
     */
    RecommendCacheSnapshot refreshRecommendCache(long userId, long pageNum, long pageSize);

    /**
     * 获取指定用户的推荐缓存状态快照
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 推荐缓存状态快照
     */
    RecommendCacheSnapshot getRecommendCacheSnapshot(long userId, long pageNum, long pageSize);

    /**
     * 根据当前缓存快照计算下一次预热刷新的延迟毫秒数
     *
     * @param cacheSnapshot 推荐缓存状态快照
     * @return 下一次刷新的延迟毫秒数
     */
    long calculateNextDelayMillis(RecommendCacheSnapshot cacheSnapshot);

    /**
     * 构建推荐用户缓存的唯一标识键
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 推荐缓存唯一标识键
     */
    String buildRecommendCacheKey(long userId, long pageNum, long pageSize);

}

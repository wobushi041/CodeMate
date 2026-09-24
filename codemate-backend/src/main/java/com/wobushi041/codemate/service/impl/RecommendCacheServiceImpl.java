package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;
import com.wobushi041.codemate.model.dto.RecommendCacheValue;
import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import com.wobushi041.codemate.service.RecommendCacheService;
import com.wobushi041.codemate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 推荐用户缓存服务实现
 *
 * @author wobushi041
 */
@Service
@Slf4j
public class RecommendCacheServiceImpl implements RecommendCacheService {

    /**
     * 最小预热刷新延迟毫秒数
     */
    private static final long MIN_DELAY_MILLIS = 1L;

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 推荐缓存 Redis 物理过期时间（分钟）
     */
    @Value("${codemate.cache.warmup.redis-ttl-minutes}")
    private long redisTtlMinutes;

    /**
     * 推荐缓存逻辑过期时长（毫秒）
     */
    @Value("${codemate.cache.warmup.logic-expire-millis}")
    private long logicExpireMillis;

    /**
     * 推荐缓存提前刷新时间窗口（毫秒）
     */
    @Value("${codemate.cache.warmup.refresh-ahead-millis}")
    private long refreshAheadMillis;

    /**
     * 推荐缓存默认兜底刷新延迟（毫秒）
     */
    @Value("${codemate.cache.warmup.delay-millis}")
    private long fallbackDelayMillis;

    /**
     * 优先从 Redisson 缓存读取推荐用户分页数据，未命中时回源查询 MySQL 并写入缓存
     *
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @param userId   当前用户 id
     * @return 推荐用户分页列表
     */
    @Override
    public Page<User> getRecommendUsers(long pageNum, long pageSize, long userId) {
        // 读取 Redis 缓存快照，若存在有效分页数据则直接返回
        RecommendCacheSnapshot cacheSnapshot = getRecommendCacheSnapshot(userId, pageNum, pageSize);
        if (cacheSnapshot.getStatus() != RecommendCacheStatus.ABSENT
                && cacheSnapshot.getCacheValue() != null
                && cacheSnapshot.getCacheValue().getUserPage() != null) {
            return cacheSnapshot.getCacheValue().getUserPage();
        }

        // 缓存未命中时回源查询数据库并写入缓存
        return queryAndCacheUsers(userId, pageNum, pageSize, false).getCacheValue().getUserPage();
    }

    /**
     * 回源查询 MySQL 并强制刷新 Redisson 中的推荐用户分页缓存
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 刷新后的推荐用户分页列表
     */
    @Override
    public Page<User> refreshRecommendUsers(long userId, long pageNum, long pageSize) {
        // 触发强制刷新并返回最新分页数据
        return refreshRecommendCache(userId, pageNum, pageSize).getCacheValue().getUserPage();
    }

    /**
     * 回源查询 MySQL 并写入带逻辑过期时间的 Redisson 缓存快照
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 刷新后的推荐缓存快照
     */
    @Override
    public RecommendCacheSnapshot refreshRecommendCache(long userId, long pageNum, long pageSize) {
        // 查询数据库并更新 Redis 缓存（缓存写入失败时抛出异常）
        return queryAndCacheUsers(userId, pageNum, pageSize, true);
    }

    /**
     * 从 Redisson 读取推荐缓存对象并评估其逻辑过期状态快照
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 推荐缓存状态快照
     */
    @Override
    public RecommendCacheSnapshot getRecommendCacheSnapshot(long userId, long pageNum, long pageSize) {
        // 构建缓存键并从 Redisson Bucket 获取原始缓存值
        String redisKey = buildRecommendCacheKey(userId, pageNum, pageSize);
        RBucket<Object> bucket = redissonClient.getBucket(redisKey);
        Object cachedValue = bucket.get();
        return buildSnapshot(redisKey, cachedValue);
    }

    /**
     * 基于缓存快照的逻辑过期时间与提前刷新窗口计算下一次调度延迟毫秒数
     *
     * @param cacheSnapshot 推荐缓存状态快照
     * @return 下一次刷新的延迟毫秒数
     */
    @Override
    public long calculateNextDelayMillis(RecommendCacheSnapshot cacheSnapshot) {
        // 快照或逻辑过期时间缺失时使用兜底延迟配置
        if (cacheSnapshot == null
                || cacheSnapshot.getCacheValue() == null
                || cacheSnapshot.getCacheValue().getLogicExpireTime() == null) {
            return Math.max(MIN_DELAY_MILLIS, fallbackDelayMillis);
        }

        // 计算距离提前刷新时间点的剩余毫秒数
        long nextDelayMillis = cacheSnapshot.getCacheValue().getLogicExpireTime()
                - System.currentTimeMillis()
                - refreshAheadMillis;
        return Math.max(MIN_DELAY_MILLIS, nextDelayMillis);
    }

    /**
     * 格式化拼接用户维度与分页参数的 Redis 缓存键名
     *
     * @param userId   当前用户 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return Redis 缓存键名
     */
    @Override
    public String buildRecommendCacheKey(long userId, long pageNum, long pageSize) {
        // 按固定前缀与用户分页参数生成 Redis Key
        return String.format("user:recommend:%d:%d:%d", userId, pageNum, pageSize);
    }

    /**
     * 解析 Redis 缓存原始值并结合逻辑过期时间戳构建缓存状态快照
     *
     * @param redisKey    Redis 缓存键名
     * @param cachedValue Redis 中存储的原始对象
     * @return 推荐缓存状态快照
     */
    @SuppressWarnings("unchecked")
    private RecommendCacheSnapshot buildSnapshot(String redisKey, Object cachedValue) {
        // 缓存为空时返回未命中状态
        if (cachedValue == null) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.ABSENT, null);
        }

        // 识别缓存对象类型并兼容旧格式分页数据
        RecommendCacheValue cacheValue;
        if (cachedValue instanceof RecommendCacheValue) {
            cacheValue = (RecommendCacheValue) cachedValue;
        }
        // 兼容旧格式 Page 缓存，视为已逻辑过期以触发平滑迁移
        else if (cachedValue instanceof Page) {
            cacheValue = new RecommendCacheValue((Page<User>) cachedValue, 0L);
        }
        // 遇到未知缓存数据结构时记录告警并按未命中处理
        else {
            log.warn("unexpected recommend cache value type, redisKey={}, valueType={}",
                    redisKey, cachedValue.getClass().getName());
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.ABSENT, null);
        }

        // 根据分页数据与逻辑过期时间戳判定缓存生命周期状态
        if (cacheValue.getUserPage() == null) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.ABSENT, null);
        }
        if (cacheValue.getLogicExpireTime() == null
                || cacheValue.getLogicExpireTime() <= System.currentTimeMillis()) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.LOGIC_EXPIRED, cacheValue);
        }
        if (refreshAheadMillis > 0
                && cacheValue.getLogicExpireTime() - System.currentTimeMillis() <= refreshAheadMillis) {
            return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.REFRESH_AHEAD, cacheValue);
        }
        return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.VALID, cacheValue);
    }

    /**
     * 分页查询数据库用户表并将结果封装逻辑过期时间写入 Redisson 缓存
     *
     * @param userId           当前用户 id
     * @param pageNum          请求页码
     * @param pageSize         每页条数
     * @param failOnCacheError 缓存写入异常时是否向上抛出异常
     * @return 最新构建的推荐缓存状态快照
     */
    private RecommendCacheSnapshot queryAndCacheUsers(long userId, long pageNum, long pageSize, boolean failOnCacheError) {
        // 从数据库分页查询推荐用户列表
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);

        // 封装逻辑过期时间并写入 Redisson Bucket
        String redisKey = buildRecommendCacheKey(userId, pageNum, pageSize);
        RBucket<Object> bucket = redissonClient.getBucket(redisKey);
        RecommendCacheValue cacheValue = new RecommendCacheValue(
                userPage,
                System.currentTimeMillis() + logicExpireMillis
        );
        try {
            bucket.set(cacheValue, redisTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("refresh recommend cache error, redisKey={}", redisKey, e);
            if (failOnCacheError) {
                throw new IllegalStateException("refresh recommend cache error");
            }
        }
        return new RecommendCacheSnapshot(redisKey, RecommendCacheStatus.VALID, cacheValue);
    }

}

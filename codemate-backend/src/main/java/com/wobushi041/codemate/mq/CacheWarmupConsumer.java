package com.wobushi041.codemate.mq;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.config.RabbitMqConfig;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;
import com.wobushi041.codemate.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import com.wobushi041.codemate.service.RecommendCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 推荐缓存预热消息消费者
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class CacheWarmupConsumer {

    /**
     * 注入推荐缓存服务依赖
     */
    @Resource
    private RecommendCacheService recommendCacheService;

    /**
     * 注入缓存预热消息生产者依赖
     */
    @Resource
    private CacheWarmupProducer cacheWarmupProducer;

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 分布式锁租约时长（秒）
     */
    @Value("${codemate.cache.warmup.lock-lease-seconds}")
    private long lockLeaseSeconds;

    /**
     * 监听缓存预热执行队列，基于分布式锁刷新推荐缓存并投递下一轮调度任务
     *
     * @param message 推荐缓存预热任务消息对象
     */
    @RabbitListener(queues = RabbitMqConfig.CACHE_WARMUP_EXECUTE_QUEUE)
    public void deliverCallback(RecommendCacheWarmupMessage message) {
        // 构造分布式锁并尝试非阻塞获取，防止并发重复刷新
        String redisKey = "cache:warmup:lock:" + message.getTaskId();
        RLock lock = redissonClient.getLock(redisKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.info("繁忙未得到锁, taskId={}, runId={}", message.getTaskId(), message.getRunId());
                return; // 未获取到锁时直接放弃本次执行，不触发重试
            }

            // 读取当前 Redis 推荐缓存快照并根据有效状态决定是否刷新
            RecommendCacheSnapshot cacheSnapshot = recommendCacheService.getRecommendCacheSnapshot(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            Page<User> userPage = null;
            boolean refreshed = false;
            if (cacheSnapshot.getStatus() != RecommendCacheStatus.VALID) {
                cacheSnapshot = recommendCacheService.refreshRecommendCache(
                        message.getUserId(),
                        message.getPageNum(),
                        message.getPageSize());
                userPage = cacheSnapshot.getCacheValue().getUserPage();
                refreshed = true;
            }
            // 当前缓存快照仍有效，直接复用现有分页结果
            else {
                userPage = cacheSnapshot.getCacheValue().getUserPage();
            }

            // 计算下一次调度延迟时间并投递下一轮延时预热消息形成闭环
            long nextDelayMillis = recommendCacheService.calculateNextDelayMillis(cacheSnapshot);
            RecommendCacheWarmupMessage nextMessage = cacheWarmupProducer.newWarmupMessage(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize()
            );
            cacheWarmupProducer.scheduleWarmupTask(nextMessage, nextDelayMillis);
            log.info("{} recommend cache, taskId={}, runId={}, status={}, total={}, nextDelayMillis={}",
                    refreshed ? "refresh" : "skip refresh",
                    message.getTaskId(),
                    message.getRunId(),
                    cacheSnapshot.getStatus(),
                    userPage == null ? 0 : userPage.getTotal(),
                    nextDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpRejectAndDontRequeueException("cache warmup interrupted", e);
        } catch (Exception e) {
            log.error("refresh recommend cache failed, taskId={}, runId={}", message.getTaskId(), message.getRunId(), e);
            throw new AmqpRejectAndDontRequeueException("cache warmup failed", e);
        } finally {
            // 释放当前线程持有的分布式锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}

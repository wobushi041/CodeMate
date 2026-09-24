package com.wobushi041.codemate.mq;

import com.wobushi041.codemate.config.RabbitMqConfig;
import com.wobushi041.codemate.model.dto.RecommendCacheWarmupMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.UUID;

/**
 * 推荐缓存预热消息生产者
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class CacheWarmupProducer {

    /**
     * 预热任务唯一标识格式化模版
     */
    private static final String TASK_ID_FORMAT = "recommend:%d:%d:%d";

    /**
     * 注入 RabbitMQ 消息模版依赖
     */
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 默认延时投递时间（毫秒）
     */
    @Value("${codemate.cache.warmup.delay-millis}")
    private long delayMillis;

    /**
     * 按配置默认延迟时间调度推荐缓存预热任务
     *
     * @param message 推荐缓存预热任务消息对象
     */
    public void scheduleWarmupTask(RecommendCacheWarmupMessage message) {
        // 使用默认延迟毫秒数发送延时消息
        sendDelayMessage(message, delayMillis);
    }

    /**
     * 按指定延迟时间调度推荐缓存预热任务
     *
     * @param message     推荐缓存预热任务消息对象
     * @param delayMillis 延迟毫秒数
     */
    public void scheduleWarmupTask(RecommendCacheWarmupMessage message, long delayMillis) {
        // 使用指定延迟毫秒数发送延时消息
        sendDelayMessage(message, delayMillis);
    }

    /**
     * 向延时交换机投递带有过期时间的持久化预热消息
     *
     * @param message     推荐缓存预热任务消息对象
     * @param delayMillis 消息延迟过期时间（毫秒）
     */
    public void sendDelayMessage(RecommendCacheWarmupMessage message, long delayMillis) {
        // 设置消息 TTL 与持久化模式并投递至延时交换机
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE,
                RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                });

        // 记录预热任务延时调度日志
        log.info("schedule warmup message, taskId={}, runId={}, delayMillis={}",
                message.getTaskId(), message.getRunId(), delayMillis);
    }

    /**
     * 构建推荐缓存预热任务消息对象
     *
     * @param userId   目标用户 id
     * @param pageNum  推荐分页页码
     * @param pageSize 推荐分页每页大小
     * @return 封装完成的推荐缓存预热消息对象
     */
    public RecommendCacheWarmupMessage newWarmupMessage(long userId, long pageNum, long pageSize) {
        // 初始化预热任务消息并设置任务标识与运行批次标识
        RecommendCacheWarmupMessage message = new RecommendCacheWarmupMessage();
        message.setTaskId(String.format(TASK_ID_FORMAT, userId, pageNum, pageSize));
        message.setRunId(UUID.randomUUID().toString());
        message.setUserId(userId);
        message.setPageNum(pageNum);
        message.setPageSize(pageSize);
        message.setCreateTime(System.currentTimeMillis());
        return message;
    }

}

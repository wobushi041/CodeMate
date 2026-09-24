package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.chat.ChatMessageResponse;
import com.wobushi041.codemate.mapper.ChatMessageMapper;
import com.wobushi041.codemate.model.domain.ChatMessage;
import com.wobushi041.codemate.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 聊天消息服务实现
 *
 * @author wobushi041
 */
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    /**
     * 兜底消息在 Redis 中的过期时间
     */
    private static final Duration FALLBACK_TTL = Duration.ofDays(1);

    /**
     * 兜底消息 Redis 键名前缀
     */
    private static final String FALLBACK_KEY_PREFIX = "chat:message:fallback:";

    /**
     * 消息时间格式化模式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 注入聊天消息持久层依赖
     */
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 注入 Redis 模板依赖
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存聊天消息至 MySQL 数据库并回填消息 id
     *
     * @param message 聊天消息响应对象
     */
    @Override
    public void saveMessage(ChatMessageResponse message) {
        // 组装聊天消息实体
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setTeamId(message.getTeamId());
        chatMessage.setClientMessageId(message.getClientMessageId());
        chatMessage.setFromUserId(message.getFromUserId());
        chatMessage.setFromUsername(message.getFromUsername());
        chatMessage.setContent(message.getContent());
        chatMessage.setCreateTime(parseDate(message.getCreateTime()));

        // 执行数据库插入并回填主键 id
        chatMessageMapper.insert(chatMessage);
        message.setMessageId(chatMessage.getId());
    }

    /**
     * 兜底保存未成功落库的聊天消息至 Redis
     *
     * @param message 聊天消息响应对象
     */
    @Override
    public void saveFallbackMessage(ChatMessageResponse message) {
        // 兜底策略，将未落库消息暂存至 Redis 列表并设置过期时间
        String key = fallbackKey(message.getTeamId());
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, FALLBACK_TTL);
    }

    /**
     * 分页查询 MySQL 队伍历史聊天消息并合并 Redis 兜底消息
     *
     * @param teamId   队伍 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 队伍聊天消息分页响应结果
     */
    @Override
    public Page<ChatMessageResponse> listTeamMessagesWithFallback(Long teamId, long pageNum, long pageSize) {
        // 分页查询数据库历史消息
        Page<ChatMessage> messagePage = chatMessageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<ChatMessage>()
                        .eq("teamId", teamId)
                        .orderByDesc("createTime")
        );

        // 转换为响应结构列表
        List<ChatMessageResponse> records = new ArrayList<>();
        for (ChatMessage chatMessage : messagePage.getRecords()) {
            records.add(toResponse(chatMessage));
        }

        // 查询第 1 页时合并 Redis 中的兜底消息并按时间倒序重排
        List<ChatMessageResponse> fallbackMessages = new ArrayList<>();
        if (pageNum == 1) {
            fallbackMessages = listFallbackMessages(teamId);
            records.addAll(fallbackMessages);
            records.sort(Comparator.comparing(ChatMessageResponse::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        }

        // 构造分页结果返回
        Page<ChatMessageResponse> responsePage = new Page<>(pageNum, pageSize);
        responsePage.setRecords(records);
        responsePage.setTotal(messagePage.getTotal() + fallbackMessages.size());
        return responsePage;
    }

    /**
     * 获取指定队伍在 Redis 中的兜底消息列表
     *
     * @param teamId 队伍 id
     * @return 兜底聊天消息响应列表
     */
    private List<ChatMessageResponse> listFallbackMessages(Long teamId) {
        // 从 Redis 列表中读取全量兜底消息
        List<Object> values = redisTemplate.opsForList().range(fallbackKey(teamId), 0, -1);
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤并收集类型为 ChatMessageResponse 的消息对象
        List<ChatMessageResponse> messages = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof ChatMessageResponse response) {
                messages.add(response);
            }
        }
        return messages;
    }

    /**
     * 将聊天消息实体转换为消息响应对象
     *
     * @param chatMessage 聊天消息实体
     * @return 聊天消息响应对象
     */
    private ChatMessageResponse toResponse(ChatMessage chatMessage) {
        // 映射数据库实体属性至下行响应结构
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("CHAT");
        response.setTeamId(chatMessage.getTeamId());
        response.setMessageId(chatMessage.getId());
        response.setClientMessageId(chatMessage.getClientMessageId());
        response.setFromUserId(chatMessage.getFromUserId());
        response.setFromUsername(chatMessage.getFromUsername());
        response.setContent(chatMessage.getContent());
        response.setCreateTime(formatDate(chatMessage.getCreateTime()));
        return response;
    }

    /**
     * 构造指定队伍的兜底消息 Redis 键名
     *
     * @param teamId 队伍 id
     * @return 完整的 Redis 键名
     */
    private String fallbackKey(Long teamId) {
        // 拼接兜底消息前缀与队伍 id
        return FALLBACK_KEY_PREFIX + teamId;
    }

    /**
     * 解析时间字符串为日期对象
     *
     * @param createTime 时间字符串
     * @return 解析后的 Date 对象（解析失败时返回当前时间）
     */
    private Date parseDate(String createTime) {
        // 按标准日期格式解析时间字符串，解析异常时降级返回当前系统时间
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(createTime);
        } catch (Exception e) {
            return new Date();
        }
    }

    /**
     * 格式化日期对象为时间字符串
     *
     * @param date 日期对象
     * @return 格式化后的时间字符串
     */
    private String formatDate(Date date) {
        // 判空并按标准模式格式化时间
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }

}

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

import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Duration FALLBACK_TTL = Duration.ofDays(1);
    private static final String FALLBACK_KEY_PREFIX = "chat:message:fallback:";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final ChatMessageMapper chatMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveMessage(ChatMessageResponse message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setTeamId(message.getTeamId());
        chatMessage.setClientMessageId(message.getClientMessageId());
        chatMessage.setFromUserId(message.getFromUserId());
        chatMessage.setFromUsername(message.getFromUsername());
        chatMessage.setContent(message.getContent());
        chatMessage.setCreateTime(parseDate(message.getCreateTime()));
        //执行数据库插入操作
        chatMessageMapper.insert(chatMessage);
        message.setMessageId(chatMessage.getId());
    }

    //兜底策略，redis存储message
    @Override
    public void saveFallbackMessage(ChatMessageResponse message) {
        String key = fallbackKey(message.getTeamId());
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, FALLBACK_TTL);
    }

    @Override
    public Page<ChatMessageResponse> listTeamMessagesWithFallback(Long teamId, long pageNum, long pageSize) {
        Page<ChatMessage> messagePage = chatMessageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<ChatMessage>()
                        .eq("teamId", teamId)
                        .orderByDesc("createTime")
        );
        List<ChatMessageResponse> records = new ArrayList<>();
        for (ChatMessage chatMessage : messagePage.getRecords()) {
            records.add(toResponse(chatMessage));
        }
        List<ChatMessageResponse> fallbackMessages = new ArrayList<>();
        if (pageNum == 1) {
            fallbackMessages = listFallbackMessages(teamId);
            records.addAll(fallbackMessages);
            records.sort(Comparator.comparing(ChatMessageResponse::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        }
        Page<ChatMessageResponse> responsePage = new Page<>(pageNum, pageSize);
        responsePage.setRecords(records);
        responsePage.setTotal(messagePage.getTotal() + fallbackMessages.size());
        return responsePage;
    }

    private List<ChatMessageResponse> listFallbackMessages(Long teamId) {
        List<Object> values = redisTemplate.opsForList().range(fallbackKey(teamId), 0, -1);
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChatMessageResponse> messages = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof ChatMessageResponse response) {
                messages.add(response);
            }
        }
        return messages;
    }

    private ChatMessageResponse toResponse(ChatMessage chatMessage) {
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

    private String fallbackKey(Long teamId) {
        return FALLBACK_KEY_PREFIX + teamId;
    }

    private Date parseDate(String createTime) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(createTime);
        } catch (Exception e) {
            return new Date();
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}

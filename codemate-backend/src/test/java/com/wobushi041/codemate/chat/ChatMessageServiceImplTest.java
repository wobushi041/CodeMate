package com.wobushi041.codemate.chat;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.mapper.ChatMessageMapper;
import com.wobushi041.codemate.model.domain.ChatMessage;
import com.wobushi041.codemate.service.impl.ChatMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 聊天消息服务实现单元测试
 *
 * @author wobushi041
 */
class ChatMessageServiceImplTest {

    /**
     * 模拟聊天消息 Mapper 依赖
     */
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);

    /**
     * 模拟 Redis 模板依赖
     */
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    /**
     * 模拟 Redis 列表操作依赖
     */
    private final ListOperations<String, Object> listOperations = mock(ListOperations.class);

    /**
     * 待测聊天消息服务实现实例
     */
    private final ChatMessageServiceImpl chatMessageService = new ChatMessageServiceImpl(chatMessageMapper, redisTemplate);

    /**
     * 测试保存聊天消息至 MySQL
     */
    // 场景：测试聊天消息正常持久化写入 MySQL 数据库
    @Test
    void savesChatMessageToMysql() {
        // 1. 准备测试数据
        ChatMessageResponse response = chatResponse();

        // 2. 调用待测方法
        chatMessageService.saveMessage(response);

        // 3. 断言结果
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getFromUsername());
        assertEquals("tester", captor.getValue().getFromUsername());
    }

    /**
     * 测试保存兜底聊天消息至 Redis 并设置 1 天过期时间
     */
    // 场景：测试将异常兜底消息写入 Redis 列表并设置 1 天 TTL
    @Test
    void savesFallbackMessageToRedisWithOneDayTtl() {
        // 1. 准备测试数据与模拟依赖
        ChatMessageResponse response = chatResponse();
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        // 2. 调用待测方法
        chatMessageService.saveFallbackMessage(response);

        // 3. 断言结果
        verify(listOperations).rightPush("chat:message:fallback:1", response);
        verify(redisTemplate).expire("chat:message:fallback:1", Duration.ofDays(1));
    }

    /**
     * 测试第一页查询合并 MySQL 历史消息与 Redis 兜底消息
     */
    // 场景：测试分页查询第 1 页时合并 MySQL 消息记录与 Redis 兜底消息
    @Test
    void listsMysqlMessagesAndFallbackMessagesOnFirstPage() {
        // 1. 准备测试数据与模拟依赖
        ChatMessage dbMessage = new ChatMessage();
        dbMessage.setId(100L);
        dbMessage.setTeamId(1L);
        dbMessage.setFromUserId(41L);
        dbMessage.setFromUsername("tester");
        dbMessage.setContent("mysql message");
        Page<ChatMessage> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of(dbMessage));
        dbPage.setTotal(1);
        ChatMessageResponse fallback = chatResponse();
        fallback.setContent("fallback message");
        when(chatMessageMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(dbPage);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(eq("chat:message:fallback:1"), eq(0L), eq(-1L))).thenReturn(List.of(fallback));

        // 2. 调用待测方法
        Page<ChatMessageResponse> result = chatMessageService.listTeamMessagesWithFallback(1L, 1, 20);

        // 3. 断言结果
        assertEquals(2, result.getRecords().size());
        assertEquals(2, result.getTotal());
    }

    /**
     * 测试查询 MySQL 消息时保留发送者用户名字段
     */
    // 场景：测试从 MySQL 查询历史消息并转换为响应对象时保留发送者用户名
    @Test
    void keepsFromUsernameFromMysqlMessages() {
        // 1. 准备测试数据与模拟依赖
        ChatMessage dbMessage = new ChatMessage();
        dbMessage.setId(100L);
        dbMessage.setTeamId(1L);
        dbMessage.setFromUserId(41L);
        dbMessage.setFromUsername("tester");
        dbMessage.setContent("mysql message");
        Page<ChatMessage> dbPage = new Page<>(1, 20);
        dbPage.setRecords(List.of(dbMessage));
        dbPage.setTotal(1);
        when(chatMessageMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(dbPage);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(eq("chat:message:fallback:1"), eq(0L), eq(-1L))).thenReturn(List.of());

        // 2. 调用待测方法
        Page<ChatMessageResponse> result = chatMessageService.listTeamMessagesWithFallback(1L, 1, 20);

        // 3. 断言结果
        assertEquals("tester", result.getRecords().get(0).getFromUsername());
    }

    /**
     * 构造测试用聊天消息响应对象
     *
     * @return 聊天消息响应对象
     */
    private ChatMessageResponse chatResponse() {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("CHAT");
        response.setTeamId(1L);
        response.setFromUserId(41L);
        response.setFromUsername("tester");
        response.setClientMessageId("uuid-1");
        response.setContent("hello");
        response.setCreateTime("2026-09-01 20:00:00");
        return response;
    }

}

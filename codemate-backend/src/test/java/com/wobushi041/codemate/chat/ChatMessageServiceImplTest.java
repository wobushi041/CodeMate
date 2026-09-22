package com.wobushi041.codemate.chat;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.mapper.ChatMessageMapper;
import com.wobushi041.codemate.model.domain.ChatMessage;
import com.wobushi041.codemate.service.impl.ChatMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessageServiceImplTest {

    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final ListOperations<String, Object> listOperations = mock(ListOperations.class);
    private final ChatMessageServiceImpl chatMessageService = new ChatMessageServiceImpl(chatMessageMapper, redisTemplate);

    @Test
    void savesChatMessageToMysql() {
        ChatMessageResponse response = chatResponse();

        chatMessageService.saveMessage(response);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getFromUsername());
        assertEquals("tester", captor.getValue().getFromUsername());
    }

    @Test
    void savesFallbackMessageToRedisWithOneDayTtl() {
        ChatMessageResponse response = chatResponse();
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        chatMessageService.saveFallbackMessage(response);

        verify(listOperations).rightPush("chat:message:fallback:1", response);
        verify(redisTemplate).expire("chat:message:fallback:1", Duration.ofDays(1));
    }

    @Test
    void listsMysqlMessagesAndFallbackMessagesOnFirstPage() {
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

        Page<ChatMessageResponse> result = chatMessageService.listTeamMessagesWithFallback(1L, 1, 20);

        assertEquals(2, result.getRecords().size());
        assertEquals(2, result.getTotal());
    }

    @Test
    void keepsFromUsernameFromMysqlMessages() {
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

        Page<ChatMessageResponse> result = chatMessageService.listTeamMessagesWithFallback(1L, 1, 20);

        assertEquals("tester", result.getRecords().get(0).getFromUsername());
    }

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

package com.wobushi041.codemate.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.PrivateChatService;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * WebSocket 聊天消息分发处理器单元测试
 *
 * @author wobushi041
 */
class ChatConnectionHandlerTest {

    /**
     * 验证私聊发送帧缺少 sessionId 时使用当前连接已绑定的会话 ID
     */
    @Test
    void restoresBoundSessionIdForPrivateChatMessage() {
        ChatChannelManager channelManager = mock(ChatChannelManager.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);
        PrivateChatService privateChatService = mock(PrivateChatService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        EmbeddedChannel channel = new EmbeddedChannel(new ChatConnectionHandler(
                channelManager, chatRoomService, privateChatService, objectMapper));

        User loginUser = new User();
        loginUser.setId(41L);
        loginUser.setUsername("mini-program-user");
        channel.attr(ChatAttributes.LOGIN_USER).set(loginUser);

        channel.writeInbound(new TextWebSocketFrame("{\"type\":\"PRIVATE_JOIN\",\"sessionId\":88}"));
        TextWebSocketFrame joinedResponse = channel.readOutbound();
        assertEquals("PRIVATE_JOINED", readResponse(objectMapper, joinedResponse).getType());
        joinedResponse.release();

        channel.writeInbound(new TextWebSocketFrame(
                "{\"type\":\"PRIVATE_CHAT\",\"clientMessageId\":\"client-1\",\"content\":\"你好\"}"));

        ArgumentCaptor<ChatInboundMessage> requestCaptor = ArgumentCaptor.forClass(ChatInboundMessage.class);
        verify(privateChatService).sendPrivateMessage(eq(loginUser), eq(channel), requestCaptor.capture());
        assertEquals(88L, requestCaptor.getValue().getSessionId());

        channel.finishAndReleaseAll();
    }

    private ChatMessageResponse readResponse(ObjectMapper objectMapper, TextWebSocketFrame frame) {
        try {
            return objectMapper.readValue(frame.text(), ChatMessageResponse.class);
        } catch (Exception e) {
            throw new AssertionError("私聊加入响应不是合法 JSON", e);
        }
    }
}

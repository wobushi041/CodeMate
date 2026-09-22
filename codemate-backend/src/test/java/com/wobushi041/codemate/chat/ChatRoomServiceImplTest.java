package com.wobushi041.codemate.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.model.domain.Team;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.ChatMessageService;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.TeamService;
import com.wobushi041.codemate.service.UserTeamService;
import com.wobushi041.codemate.service.impl.ChatRoomServiceImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatRoomServiceImplTest {

    private final TeamService teamService = mock(TeamService.class);
    private final UserTeamService userTeamService = mock(UserTeamService.class);
    private final ChatChannelManager chatChannelManager = new ChatChannelManager();
    private final ChatMessageService chatMessageService = mock(ChatMessageService.class);
    private final ChatRoomService chatRoomService = new ChatRoomServiceImpl(
            teamService,
            userTeamService,
            chatChannelManager,
            chatMessageService,
            new ObjectMapper()
    );

    @Test
    void rejectsJoinWhenTeamDoesNotExist() {
        User loginUser = user(41L);
        EmbeddedChannel channel = new EmbeddedChannel();

        assertThrows(BusinessException.class, () -> chatRoomService.joinTeamRoom(1L, loginUser, channel));
    }

    @Test
    void rejectsJoinWhenUserIsNotTeamMember() {
        when(teamService.getById(1L)).thenReturn(new Team());
        when(userTeamService.count(any())).thenReturn(0L);

        User loginUser = user(41L);
        EmbeddedChannel channel = new EmbeddedChannel();

        assertThrows(BusinessException.class, () -> chatRoomService.joinTeamRoom(1L, loginUser, channel));
    }

    @Test
    void joinsRoomWhenUserIsTeamMember() {
        when(teamService.getById(1L)).thenReturn(new Team());
        when(userTeamService.count(any())).thenReturn(1L);

        User loginUser = user(41L);
        EmbeddedChannel channel = new EmbeddedChannel();

        chatRoomService.joinTeamRoom(1L, loginUser, channel);

        assertEquals(1L, channel.attr(ChatAttributes.TEAM_ID).get());
        assertEquals(channel, chatChannelManager.get(1L, 41L));
        TextWebSocketFrame response = channel.readOutbound();
        assertNotNull(response);
        response.release();
    }

    @Test
    void broadcastsChatAndSavesFallbackWhenMysqlInsertFails() {
        User loginUser = user(41L);
        EmbeddedChannel sender = new EmbeddedChannel();
        EmbeddedChannel receiver = new EmbeddedChannel();
        sender.attr(ChatAttributes.TEAM_ID).set(1L);
        chatChannelManager.joinRoom(1L, 41L, sender);
        chatChannelManager.joinRoom(1L, 42L, receiver);
        doThrow(new RuntimeException("db down")).when(chatMessageService).saveMessage(any());

        ChatInboundMessage request = new ChatInboundMessage();
        request.setType("CHAT");
        request.setClientMessageId("uuid-1");
        request.setContent("大家好");

        chatRoomService.sendRoomMessage(loginUser, sender, request);

        assertNotNull(sender.readOutbound());
        assertNotNull(receiver.readOutbound());
        verify(chatMessageService).saveFallbackMessage(any());
    }

    @Test
    void doesNotInterruptBroadcastWhenMysqlAndRedisFallbackBothFail() {
        User loginUser = user(41L);
        EmbeddedChannel sender = new EmbeddedChannel();
        sender.attr(ChatAttributes.TEAM_ID).set(1L);
        chatChannelManager.joinRoom(1L, 41L, sender);
        doThrow(new RuntimeException("db down")).when(chatMessageService).saveMessage(any());
        doThrow(new RuntimeException("redis down")).when(chatMessageService).saveFallbackMessage(any());

        ChatInboundMessage request = new ChatInboundMessage();
        request.setType("CHAT");
        request.setContent("大家好");

        assertDoesNotThrow(() -> chatRoomService.sendRoomMessage(loginUser, sender, request));
        assertNotNull(sender.readOutbound());
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        return user;
    }
}

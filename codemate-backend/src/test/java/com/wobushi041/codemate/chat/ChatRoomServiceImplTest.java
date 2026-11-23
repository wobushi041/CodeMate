package com.wobushi041.codemate.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.mapper.UserMapper;
import com.wobushi041.codemate.mapper.UserTeamMapper;
import com.wobushi041.codemate.model.domain.Team;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.vo.TeamChatUnreadVO;
import com.wobushi041.codemate.service.ChatMessageService;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.TeamService;
import com.wobushi041.codemate.service.UserTeamService;
import com.wobushi041.codemate.service.impl.ChatRoomServiceImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 聊天室服务实现单元测试
 *
 * @author wobushi041
 */
class ChatRoomServiceImplTest {

    /**
     * 模拟队伍服务依赖
     */
    private final TeamService teamService = mock(TeamService.class);

    /**
     * 模拟用户队伍关联服务依赖
     */
    private final UserTeamService userTeamService = mock(UserTeamService.class);

    /**
     * 模拟用户队伍关联持久层依赖
     */
    private final UserTeamMapper userTeamMapper = mock(UserTeamMapper.class);

    /**
     * 聊天通道管理器实例
     */
    private final ChatChannelManager chatChannelManager = new ChatChannelManager();

    /**
     * 模拟聊天消息服务依赖
     */
    private final ChatMessageService chatMessageService = mock(ChatMessageService.class);

    /**
     * 模拟用户持久层依赖
     */
    private final UserMapper userMapper = mock(UserMapper.class);

    /**
     * 待测聊天室服务实例
     */
    private final ChatRoomService chatRoomService = new ChatRoomServiceImpl(
            teamService,
            userTeamService,
            userTeamMapper,
            chatChannelManager,
            chatMessageService,
            userMapper,
            new ObjectMapper()
    );

    /**
     * 测试队伍不存在时拒绝加入聊天室
     */
    // 场景：测试目标队伍不存在时拒绝用户加入队伍聊天室
    @Test
    void rejectsJoinWhenTeamDoesNotExist() {
        // 1. 准备测试数据
        User loginUser = user(41L);
        EmbeddedChannel channel = new EmbeddedChannel();

        // 2. 调用待测方法并捕获异常
        // 3. 断言结果
        assertThrows(BusinessException.class, () -> chatRoomService.joinTeamRoom(1L, loginUser, channel));
    }

    /**
     * 测试非队伍成员拒绝加入聊天室
     */
    // 场景：测试非队伍成员尝试加入队伍聊天室时抛出业务异常
    @Test
    void rejectsJoinWhenUserIsNotTeamMember() {
        // 1. 准备测试数据与模拟依赖
        when(teamService.getById(1L)).thenReturn(new Team());
        when(userTeamService.count(any())).thenReturn(0L);
        User loginUser = user(41L);
        EmbeddedChannel channel = new EmbeddedChannel();

        // 2. 调用待测方法并捕获异常
        // 3. 断言结果
        assertThrows(BusinessException.class, () -> chatRoomService.joinTeamRoom(1L, loginUser, channel));
    }

    /**
     * 测试合法队伍成员成功加入聊天室
     */
    // 场景：测试合法队伍成员成功加入聊天室并收到握手响应帧
    @Test
    void joinsRoomWhenUserIsTeamMember() {
        // 1. 准备测试数据与模拟依赖
        when(teamService.getById(1L)).thenReturn(new Team());
        when(userTeamService.count(any())).thenReturn(1L);
        User loginUser = user(41L);
        EmbeddedChannel channel = new EmbeddedChannel();

        // 2. 调用待测方法
        chatRoomService.joinTeamRoom(1L, loginUser, channel);

        // 3. 断言结果
        assertEquals(1L, channel.attr(ChatAttributes.TEAM_ID).get());
        assertEquals(channel, chatChannelManager.get(1L, 41L));
        TextWebSocketFrame response = channel.readOutbound();
        assertNotNull(response);
        response.release();
    }

    /**
     * 测试 MySQL 写入失败时依然广播消息并触发 Redis 兜底存储
     */
    // 场景：测试 MySQL 消息落库失败时继续广播聊天消息并调用 Redis 兜底存储
    @Test
    void broadcastsChatAndSavesFallbackWhenMysqlInsertFails() {
        // 1. 准备测试数据与模拟依赖
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

        // 2. 调用待测方法
        chatRoomService.sendRoomMessage(loginUser, sender, request);

        // 3. 断言结果
        assertNotNull(sender.readOutbound());
        assertNotNull(receiver.readOutbound());
        verify(chatMessageService).saveFallbackMessage(any());
    }

    /**
     * 测试实时群聊消息携带发送者头像地址
     */
    // 场景：测试 WebSocket 广播消息包含与首页用户资料一致的头像地址
    @Test
    void broadcastsSenderAvatarUrlWithRealtimeMessage() throws Exception {
        // 1. 准备测试数据
        User loginUser = user(41L);
        loginUser.setAvatarUrl("https://cdn.example.com/stale-avatar-41.png");
        User currentUser = user(41L);
        currentUser.setAvatarUrl("https://cdn.example.com/avatar-41.png");
        when(userMapper.selectById(41L)).thenReturn(currentUser);
        EmbeddedChannel sender = new EmbeddedChannel();
        sender.attr(ChatAttributes.TEAM_ID).set(1L);
        chatChannelManager.joinRoom(1L, 41L, sender);

        ChatInboundMessage request = new ChatInboundMessage();
        request.setType("CHAT");
        request.setClientMessageId("uuid-avatar-1");
        request.setContent("带头像的实时消息");

        // 2. 调用待测方法
        chatRoomService.sendRoomMessage(loginUser, sender, request);

        // 3. 断言广播载荷包含发送者头像地址
        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        String avatarUrl = new ObjectMapper().readTree(frame.text()).path("fromAvatarUrl").asText();
        assertEquals("https://cdn.example.com/avatar-41.png", avatarUrl);
        frame.release();
    }

    /**
     * 测试 MySQL 与 Redis 兜底均失败时不中断在线广播
     */
    // 场景：测试 MySQL 与 Redis 兜底同时异常时不阻塞房间内实时消息广播
    @Test
    void doesNotInterruptBroadcastWhenMysqlAndRedisFallbackBothFail() {
        // 1. 准备测试数据与模拟依赖
        User loginUser = user(41L);
        EmbeddedChannel sender = new EmbeddedChannel();
        sender.attr(ChatAttributes.TEAM_ID).set(1L);
        chatChannelManager.joinRoom(1L, 41L, sender);
        doThrow(new RuntimeException("db down")).when(chatMessageService).saveMessage(any());
        doThrow(new RuntimeException("redis down")).when(chatMessageService).saveFallbackMessage(any());

        ChatInboundMessage request = new ChatInboundMessage();
        request.setType("CHAT");
        request.setContent("大家好");

        // 2. 调用待测方法并验证不抛出异常
        assertDoesNotThrow(() -> chatRoomService.sendRoomMessage(loginUser, sender, request));

        // 3. 断言结果
        assertNotNull(sender.readOutbound());
    }

    /**
     * 测试查询队伍聊天室未读统计
     */
    @Test
    void listsTeamUnreadCountsForCurrentUser() {
        User loginUser = user(41L);
        TeamChatUnreadVO unread = new TeamChatUnreadVO();
        unread.setTeamId(1L);
        unread.setUnreadCount(4L);
        when(userTeamMapper.listChatUnreadCounts(41L)).thenReturn(List.of(unread));

        List<TeamChatUnreadVO> result = chatRoomService.listUnreadCounts(loginUser);

        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getUnreadCount());
    }

    /**
     * 测试进入队伍聊天室后推进已读位置
     */
    @Test
    void marksTeamRoomReadForMember() {
        when(teamService.getById(1L)).thenReturn(new Team());
        when(userTeamService.count(any())).thenReturn(1L);
        User loginUser = user(41L);

        chatRoomService.markTeamRoomRead(1L, loginUser);

        verify(userTeamMapper).markTeamChatRead(1L, 41L);
    }

    /**
     * 构造测试用户实体
     *
     * @param id 用户 id
     * @return 测试用户对象
     */
    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        return user;
    }

}

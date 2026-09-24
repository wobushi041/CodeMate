package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.chat.ChatAttributes;
import com.wobushi041.codemate.chat.ChatChannelManager;
import com.wobushi041.codemate.chat.ChatInboundMessage;
import com.wobushi041.codemate.chat.ChatMessageResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.model.domain.Team;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.domain.UserTeam;
import com.wobushi041.codemate.service.ChatMessageService;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.TeamService;
import com.wobushi041.codemate.service.UserTeamService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 队伍聊天室服务实现
 *
 * @author wobushi041
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    /**
     * 单条聊天消息最大允许字符长度
     */
    private static final int MAX_CONTENT_LENGTH = 2048;

    /**
     * 消息创建时间统一格式化模式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 注入队伍服务依赖
     */
    private final TeamService teamService;

    /**
     * 注入用户队伍关联服务依赖
     */
    private final UserTeamService userTeamService;

    /**
     * 注入 Netty 聊天通道管理依赖
     */
    private final ChatChannelManager chatChannelManager;

    /**
     * 注入聊天消息服务依赖
     */
    private final ChatMessageService chatMessageService;

    /**
     * 注入 Jackson 序列化依赖
     */
    private final ObjectMapper objectMapper;

    /**
     * 校验队伍成员身份并将客户端 Netty 通道绑定至目标队伍房间
     *
     * @param teamId    队伍 id
     * @param loginUser 当前登录用户
     * @param channel   客户端 Netty 通道
     */
    @Override
    public void joinTeamRoom(Long teamId, User loginUser, Channel channel) {
        // 校验入参合法性与队伍成员身份
        if (teamId == null || teamId <= 0 || loginUser == null || channel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入聊天室参数错误");
        }
        ensureTeamMember(teamId, loginUser.getId());

        // 若当前通道已绑定其他队伍房间，先自动退出旧房间
        Long oldTeamId = channel.attr(ChatAttributes.TEAM_ID).get();
        if (oldTeamId != null && !oldTeamId.equals(teamId)) {
            chatChannelManager.leaveRoom(oldTeamId, loginUser.getId(), channel);
        }

        // 绑定新队伍房间属性并向客户端发送入房确认帧
        channel.attr(ChatAttributes.TEAM_ID).set(teamId);
        chatChannelManager.joinRoom(teamId, loginUser.getId(), channel);
        send(channel, ChatMessageResponse.joined(teamId));
    }

    /**
     * 通过 Netty 广播队伍聊天消息并执行 MySQL 持久化与 Redis 容灾兜底
     *
     * @param loginUser 当前登录用户
     * @param channel   客户端 Netty 通道
     * @param request   客户端上行的聊天请求载荷
     */
    @Override
    public void sendRoomMessage(User loginUser, Channel channel, ChatInboundMessage request) {
        // 校验登录态、通道房间绑定状态及消息内容合法性
        if (loginUser == null || channel == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息参数错误");
        }
        Long teamId = channel.attr(ChatAttributes.TEAM_ID).get();
        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先加入聊天室");
        }
        String content = request.getContent();
        if (StringUtils.isBlank(content) || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空且不能超过2048个字符");
        }

        // 组装标准聊天广播消息体并向房间内在线客户端实时广播
        ChatMessageResponse response = buildChatResponse(teamId, loginUser, request);
        broadcast(teamId, response);

        // 优先写入 MySQL 持久化，失败时降级写入 Redis 容灾列表
        try {
            chatMessageService.saveMessage(response);
        } catch (Exception e) {
            log.error("save chat message to mysql failed, teamId={}, userId={}", teamId, loginUser.getId(), e);
            try {
                chatMessageService.saveFallbackMessage(response);
            } catch (Exception redisException) {
                log.error("save chat message fallback to redis failed, teamId={}, userId={}", teamId, loginUser.getId(), redisException);
            }
        }
    }

    /**
     * 通过数据库校验队伍存在性及用户队伍关联关系
     *
     * @param teamId 队伍 id
     * @param userId 用户 id
     */
    @Override
    public void ensureTeamMember(Long teamId, Long userId) {
        // 校验参数合法性
        if (teamId == null || teamId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询并校验队伍是否存在
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        // 查询用户与队伍关联记录以确认成员资格
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        queryWrapper.eq("userId", userId);
        long count = userTeamService.count(queryWrapper);
        if (count <= 0) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未加入队伍，不能进入聊天室");
        }
    }

    /**
     * 构建发送给客户端的聊天消息下行载荷
     *
     * @param teamId    队伍 id
     * @param loginUser 发送者用户实体
     * @param request   上行请求对象
     * @return 组装完成的聊天响应对象
     */
    private ChatMessageResponse buildChatResponse(Long teamId, User loginUser, ChatInboundMessage request) {
        // 组装下行广播消息字段
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("CHAT");
        response.setTeamId(teamId);
        response.setClientMessageId(request.getClientMessageId());
        response.setFromUserId(loginUser.getId());
        response.setFromUsername(loginUser.getUsername());
        response.setContent(request.getContent());
        response.setCreateTime(new SimpleDateFormat(DATE_PATTERN).format(new Date()));
        return response;
    }

    /**
     * 向指定队伍房间内所有活跃连接广播消息
     *
     * @param teamId   队伍 id
     * @param response 待推送的聊天消息对象
     */
    private void broadcast(Long teamId, ChatMessageResponse response) {
        // 序列化消息并遍历房间内活跃通道逐一推送
        String message = toJson(response);
        Map<Long, Channel> roomChannels = chatChannelManager.getRoomChannels(teamId);
        for (Channel roomChannel : roomChannels.values()) {
            if (roomChannel != null && roomChannel.isActive()) {
                roomChannel.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }

    /**
     * 向单个指定 Channel 发送响应帧
     *
     * @param channel  目标客户端连接通道
     * @param response 响应数据对象
     */
    private void send(Channel channel, ChatMessageResponse response) {
        // 校验通道活跃状态后写出 WebSocket 文本帧
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(toJson(response)));
        }
    }

    /**
     * 将聊天消息响应对象序列化为 JSON 字符串
     *
     * @param response 响应对象
     * @return JSON 字符串
     */
    private String toJson(ChatMessageResponse response) {
        // 调用 Jackson 执行 JSON 序列化
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息序列化失败");
        }
    }

}

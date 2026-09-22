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
 * 队伍聊天室核心业务服务实现类
 *
 * 负责队伍聊天室进房校验、群聊消息构建与广播分发，并驱动 MySQL 消息持久化及 Redis 异常容灾兜底。
 *
 * @author 硫酸铜
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
     * 队伍基础服务
     */
    private final TeamService teamService;

    /**
     * 用户-队伍关联关系服务，用于队伍成员身份判定
     */
    private final UserTeamService userTeamService;

    /**
     * Netty 聊天通道会话管理器
     */
    private final ChatChannelManager chatChannelManager;

    /**
     * 聊天消息持久化与缓存降级服务
     */
    private final ChatMessageService chatMessageService;

    /**
     * Jackson JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 用户加入指定队伍聊天室
     *
     * 校验入参及队伍成员合法性，若用户此前已在其他队伍房间中，先自动退出旧房间，再加入新房间并返回确认响应。
     *
     * @param teamId    队伍 ID
     * @param loginUser 当前登录用户
     * @param channel   客户端 Netty 通道
     */
    @Override
    public void joinTeamRoom(Long teamId, User loginUser, Channel channel) {
        if (teamId == null || teamId <= 0 || loginUser == null || channel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入聊天室参数错误");
        }
        ensureTeamMember(teamId, loginUser.getId());
        Long oldTeamId = channel.attr(ChatAttributes.TEAM_ID).get();
        if (oldTeamId != null && !oldTeamId.equals(teamId)) {
            chatChannelManager.leaveRoom(oldTeamId, loginUser.getId(), channel);
        }
        channel.attr(ChatAttributes.TEAM_ID).set(teamId);
        chatChannelManager.joinRoom(teamId, loginUser.getId(), channel);
        send(channel, ChatMessageResponse.joined(teamId));
    }

    /**
     * 发送队伍聊天消息
     *
     * 校验用户房间归属及内容合法性后，立即向当前队伍所有在线成员广播消息帧；
     * 紧接着执行双通道持久化：优先写入 MySQL，若写入异常则降级写入 Redis List 容灾缓存。
     *
     * @param loginUser 当前登录用户
     * @param channel   客户端 Netty 通道
     * @param request   客户端上行的聊天请求载荷
     */
    @Override
    public void sendRoomMessage(User loginUser, Channel channel, ChatInboundMessage request) {
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
        // 1. 组装标准聊天广播消息体
        ChatMessageResponse response = buildChatResponse(teamId, loginUser, request);
        // 2. 实时广播给当前队伍中所有在线客户端
        broadcast(teamId, response);
        // 3. 异步持久化及双通道容灾
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
     * 校验指定用户是否属于指定队伍
     *
     * @param teamId 队伍 ID
     * @param userId 用户 ID
     * @throws BusinessException 参数错误、队伍不存在或未加入队伍时抛出业务异常
     */
    @Override
    public void ensureTeamMember(Long teamId, Long userId) {
        if (teamId == null || teamId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
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
     * @param teamId    队伍 ID
     * @param loginUser 发送者用户实体
     * @param request   上行请求对象
     * @return 组装完成的聊天响应对象
     */
    private ChatMessageResponse buildChatResponse(Long teamId, User loginUser, ChatInboundMessage request) {
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
     * @param teamId   队伍 ID
     * @param response 待推送的聊天消息对象
     */
    private void broadcast(Long teamId, ChatMessageResponse response) {
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
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(toJson(response)));
        }
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param response 响应对象
     * @return JSON 字符串
     */
    private String toJson(ChatMessageResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息序列化失败");
        }
    }
}

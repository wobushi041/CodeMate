package com.wobushi041.matchsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.matchsystem.chat.ChatAttributes;
import com.wobushi041.matchsystem.chat.ChatChannelManager;
import com.wobushi041.matchsystem.chat.ChatInboundMessage;
import com.wobushi041.matchsystem.chat.ChatMessageResponse;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.model.domain.Team;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.domain.UserTeam;
import com.wobushi041.matchsystem.service.ChatMessageService;
import com.wobushi041.matchsystem.service.ChatRoomService;
import com.wobushi041.matchsystem.service.TeamService;
import com.wobushi041.matchsystem.service.UserTeamService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private static final int MAX_CONTENT_LENGTH = 2048;
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final TeamService teamService;
    private final UserTeamService userTeamService;
    private final ChatChannelManager chatChannelManager;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

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
        //组装返回对象
        ChatMessageResponse response = buildChatResponse(teamId, loginUser, request);
        broadcast(teamId, response);
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
   //广播，涉及序列化
    private void broadcast(Long teamId, ChatMessageResponse response) {
        String message = toJson(response);
        Map<Long, Channel> roomChannels = chatChannelManager.getRoomChannels(teamId);
        for (Channel roomChannel : roomChannels.values()) {
            if (roomChannel != null && roomChannel.isActive()) {
                roomChannel.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }

    private void send(Channel channel, ChatMessageResponse response) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(toJson(response)));
        }
    }

    private String toJson(ChatMessageResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息序列化失败");
        }
    }
}

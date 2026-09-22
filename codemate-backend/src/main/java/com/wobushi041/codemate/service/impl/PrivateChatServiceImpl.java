package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.chat.ChatAttributes;
import com.wobushi041.codemate.chat.ChatChannelManager;
import com.wobushi041.codemate.chat.ChatInboundMessage;
import com.wobushi041.codemate.chat.ChatMessageResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.mapper.PrivateChatMessageMapper;
import com.wobushi041.codemate.mapper.PrivateChatSessionMapper;
import com.wobushi041.codemate.model.domain.PrivateChatMessage;
import com.wobushi041.codemate.model.domain.PrivateChatSession;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.vo.PrivateChatSessionVO;
import com.wobushi041.codemate.model.vo.UserVO;
import com.wobushi041.codemate.service.PrivateChatService;
import com.wobushi041.codemate.service.UserService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 单人私聊核心业务服务实现类
 *
 * @author wobushi041
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatSessionMapper, PrivateChatSession>
        implements PrivateChatService {

    private static final int MAX_CONTENT_LENGTH = 2048;
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String REDIS_FALLBACK_PREFIX = "chat:private:fallback:";
    private static final Duration FALLBACK_TTL = Duration.ofDays(1);

    private final PrivateChatSessionMapper privateChatSessionMapper;
    private final PrivateChatMessageMapper privateChatMessageMapper;
    private final UserService userService;
    private final ChatChannelManager chatChannelManager;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 发起私聊会话（联系我），开启或复用单人聊天室
     */
    @Override
    public PrivateChatSessionVO startSession(Long targetUserId, User loginUser) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户参数错误");
        }
        if (loginUser == null || loginUser.getId() <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        if (targetUserId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能与自己发起私聊");
        }

        // 1. 查询目标用户是否存在且状态正常
        User targetUser = userService.getById(targetUserId);
        if (targetUser == null || (targetUser.getIsDelete() != null && targetUser.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "目标用户不存在或已注销");
        }

        // 2. 规范化 user1Id (较小值) 与 user2Id (较大值)，保证双向唯一
        long user1Id = Math.min(loginUser.getId(), targetUserId);
        long user2Id = Math.max(loginUser.getId(), targetUserId);

        // 3. 查询是否已存在会话
        LambdaQueryWrapper<PrivateChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PrivateChatSession::getUser1Id, user1Id)
                .eq(PrivateChatSession::getUser2Id, user2Id);
        PrivateChatSession session = privateChatSessionMapper.selectOne(queryWrapper);

        // 4. 若不存在则创建新会话
        if (session == null) {
            session = new PrivateChatSession();
            session.setUser1Id(user1Id);
            session.setUser2Id(user2Id);
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());
            privateChatSessionMapper.insert(session);
        }

        // 5. 组装返回 VO
        PrivateChatSessionVO sessionVO = new PrivateChatSessionVO();
        sessionVO.setSessionId(session.getId());

        UserVO targetUserVO = new UserVO();
        BeanUtils.copyProperties(targetUser, targetUserVO);
        sessionVO.setTargetUser(targetUserVO);

        // 实时从 Netty 获取对方是否在线
        sessionVO.setIsTargetOnline(chatChannelManager.isUserOnline(targetUserId));
        sessionVO.setLastMessage(session.getLastMessage());
        sessionVO.setLastMessageTime(formatDate(session.getLastMessageTime()));
        sessionVO.setCreateTime(formatDate(session.getCreateTime()));

        return sessionVO;
    }

    /**
     * 发送单人私聊消息（支持离线持久化与在线点对点实时推送）
     */
    @Override
    public void sendPrivateMessage(User loginUser, Channel channel, ChatInboundMessage request) {
        if (loginUser == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息参数错误");
        }
        String content = request.getContent();
        if (StringUtils.isBlank(content) || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空且不能超过2048字符");
        }

        Long sessionId = request.getSessionId();
        Long toUserId = request.getToUserId();

        // 若未提供 sessionId 但提供了 toUserId，则自动解析出 sessionId
        PrivateChatSession session;
        if (sessionId != null && sessionId > 0) {
            session = ensureSessionParticipant(sessionId, loginUser.getId());
            if (toUserId == null) {
                toUserId = session.getUser1Id().equals(loginUser.getId()) ? session.getUser2Id() : session.getUser1Id();
            }
        } else if (toUserId != null && toUserId > 0) {
            if (toUserId.equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能向自己发送私聊消息");
            }
            long u1 = Math.min(loginUser.getId(), toUserId);
            long u2 = Math.max(loginUser.getId(), toUserId);
            LambdaQueryWrapper<PrivateChatSession> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PrivateChatSession::getUser1Id, u1).eq(PrivateChatSession::getUser2Id, u2);
            session = privateChatSessionMapper.selectOne(queryWrapper);
            if (session == null) {
                session = new PrivateChatSession();
                session.setUser1Id(u1);
                session.setUser2Id(u2);
                session.setCreateTime(new Date());
                privateChatSessionMapper.insert(session);
            }
            sessionId = session.getId();
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少会话 ID 或目标接收者 ID");
        }

        Date now = new Date();
        String formattedTime = formatDate(now);

        // 1. 离线保持：构建实体并优先插入 MySQL
        PrivateChatMessage privateChatMessage = new PrivateChatMessage();
        privateChatMessage.setSessionId(sessionId);
        privateChatMessage.setClientMessageId(request.getClientMessageId());
        privateChatMessage.setFromUserId(loginUser.getId());
        privateChatMessage.setToUserId(toUserId);
        privateChatMessage.setContent(content);
        privateChatMessage.setIsRead(0);
        privateChatMessage.setCreateTime(now);

        try {
            privateChatMessageMapper.insert(privateChatMessage);
            // 更新会话最后消息与时间
            session.setLastMessage(content);
            session.setLastMessageTime(now);
            privateChatSessionMapper.updateById(session);
        } catch (Exception e) {
            log.error("save private chat message to mysql failed, sessionId={}, fromUserId={}, toUserId={}",
                    sessionId, loginUser.getId(), toUserId, e);
            // 异常降级写入 Redis 容灾缓存
            saveFallbackMessage(sessionId, privateChatMessage);
        }

        // 2. 构建下行标准响应帧
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("PRIVATE_CHAT");
        response.setSessionId(sessionId);
        response.setMessageId(privateChatMessage.getId());
        response.setClientMessageId(request.getClientMessageId());
        response.setFromUserId(loginUser.getId());
        response.setFromUsername(loginUser.getUsername());
        response.setToUserId(toUserId);
        response.setContent(content);
        response.setCreateTime(formattedTime);

        // 3. 给发送方自身 Channel 投递确认回执（ACK）
        if (channel != null && channel.isActive()) {
            sendFrame(channel, response);
        }

        // 4. 在线即时通信：检查接收方 Channel 是否在线
        Channel targetChannel = chatChannelManager.getUserChannel(toUserId);
        if (targetChannel != null && targetChannel.isActive()) {
            sendFrame(targetChannel, response);
            log.info("private message delivered online: fromUserId={} -> toUserId={}, sessionId={}",
                    loginUser.getId(), toUserId, sessionId);
        } else {
            log.info("target user is offline, message saved for offline pickup: toUserId={}, sessionId={}",
                    toUserId, sessionId);
        }
    }

    /**
     * 分页查询指定私聊会话的历史聊天记录
     */
    @Override
    public Page<ChatMessageResponse> listSessionMessages(Long sessionId, long pageNum, long pageSize, User loginUser) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 ID 参数错误");
        }
        if (loginUser == null || loginUser.getId() <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        // 1. 严格防越权校验：必须为该会话的成员
        ensureSessionParticipant(sessionId, loginUser.getId());

        // 2. 标记发给当前用户的未读消息为已读
        try {
            LambdaUpdateWrapper<PrivateChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(PrivateChatMessage::getSessionId, sessionId)
                    .eq(PrivateChatMessage::getToUserId, loginUser.getId())
                    .eq(PrivateChatMessage::getIsRead, 0)
                    .set(PrivateChatMessage::getIsRead, 1);
            privateChatMessageMapper.update(null, updateWrapper);
        } catch (Exception e) {
            log.warn("mark private messages as read failed, sessionId={}, userId={}", sessionId, loginUser.getId(), e);
        }

        // 3. 分页倒序查询历史消息
        Page<PrivateChatMessage> messagePage = privateChatMessageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PrivateChatMessage>()
                        .eq(PrivateChatMessage::getSessionId, sessionId)
                        .orderByDesc(PrivateChatMessage::getCreateTime)
        );

        List<ChatMessageResponse> records = new ArrayList<>();
        for (PrivateChatMessage item : messagePage.getRecords()) {
            records.add(toResponse(item));
        }

        // 4. 第一页合并 Redis 容灾兜底缓存（如果有）
        if (pageNum == 1) {
            List<ChatMessageResponse> fallbackList = listFallbackMessages(sessionId);
            if (!fallbackList.isEmpty()) {
                records.addAll(fallbackList);
                records.sort(Comparator.comparing(ChatMessageResponse::getCreateTime,
                        Comparator.nullsLast(String::compareTo)).reversed());
            }
        }

        Page<ChatMessageResponse> responsePage = new Page<>(pageNum, pageSize);
        responsePage.setRecords(records);
        responsePage.setTotal(messagePage.getTotal());
        return responsePage;
    }

    /**
     * 校验指定用户是否为指定会话的合法参与方
     */
    @Override
    public PrivateChatSession ensureSessionParticipant(Long sessionId, Long userId) {
        if (sessionId == null || sessionId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PrivateChatSession session = privateChatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "私聊会话不存在");
        }
        if (!userId.equals(session.getUser1Id()) && !userId.equals(session.getUser2Id())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权访问此私聊会话");
        }
        return session;
    }

    private void saveFallbackMessage(Long sessionId, PrivateChatMessage message) {
        try {
            String key = REDIS_FALLBACK_PREFIX + sessionId;
            ChatMessageResponse response = toResponse(message);
            redisTemplate.opsForList().rightPush(key, response);
            redisTemplate.expire(key, FALLBACK_TTL);
        } catch (Exception e) {
            log.error("save private fallback message to redis failed, sessionId={}", sessionId, e);
        }
    }

    private List<ChatMessageResponse> listFallbackMessages(Long sessionId) {
        try {
            String key = REDIS_FALLBACK_PREFIX + sessionId;
            List<Object> values = redisTemplate.opsForList().range(key, 0, -1);
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
        } catch (Exception e) {
            log.warn("get fallback messages from redis failed, sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    private ChatMessageResponse toResponse(PrivateChatMessage item) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("PRIVATE_CHAT");
        response.setSessionId(item.getSessionId());
        response.setMessageId(item.getId());
        response.setClientMessageId(item.getClientMessageId());
        response.setFromUserId(item.getFromUserId());
        response.setToUserId(item.getToUserId());
        response.setContent(item.getContent());
        response.setCreateTime(formatDate(item.getCreateTime()));
        return response;
    }

    private void sendFrame(Channel channel, ChatMessageResponse response) {
        try {
            channel.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        } catch (JsonProcessingException e) {
            log.error("serialize websocket message failed", e);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}

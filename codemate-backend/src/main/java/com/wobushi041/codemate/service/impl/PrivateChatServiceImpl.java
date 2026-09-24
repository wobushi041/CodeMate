package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 单人私聊服务实现
 *
 * @author wobushi041
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatSessionMapper, PrivateChatSession>
        implements PrivateChatService {

    /**
     * 单条私聊消息最大允许字符长度
     */
    private static final int MAX_CONTENT_LENGTH = 2048;

    /**
     * 消息时间格式化模式
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 私聊兜底消息 Redis 键名前缀
     */
    private static final String REDIS_FALLBACK_PREFIX = "chat:private:fallback:";

    /**
     * 私聊兜底消息在 Redis 中的过期时间
     */
    private static final Duration FALLBACK_TTL = Duration.ofDays(1);

    /**
     * 注入私聊会话持久层依赖
     */
    private final PrivateChatSessionMapper privateChatSessionMapper;

    /**
     * 注入私聊消息持久层依赖
     */
    private final PrivateChatMessageMapper privateChatMessageMapper;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入 Netty 聊天通道管理依赖
     */
    private final ChatChannelManager chatChannelManager;

    /**
     * 注入 Jackson 序列化依赖
     */
    private final ObjectMapper objectMapper;

    /**
     * 注入 Redis 模板依赖
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询或创建 MySQL 单人私聊会话并结合 Netty 在线状态组装会话视图
     *
     * @param targetUserId 目标联系用户 id
     * @param loginUser    当前登录用户
     * @return 私聊会话视图对象
     */
    @Override
    public PrivateChatSessionVO startSession(Long targetUserId, User loginUser) {
        // 校验当前用户与目标用户参数合法性
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户参数错误");
        }
        if (loginUser == null || loginUser.getId() <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        if (targetUserId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能与自己发起私聊");
        }

        // 查询目标用户是否存在且状态正常
        User targetUser = userService.getById(targetUserId);
        if (targetUser == null || (targetUser.getIsDelete() != null && targetUser.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "目标用户不存在或已注销");
        }

        // 规范化 user1Id（较小值）与 user2Id（较大值），保证会话双向唯一
        long user1Id = Math.min(loginUser.getId(), targetUserId);
        long user2Id = Math.max(loginUser.getId(), targetUserId);

        // 查询是否已存在私聊会话记录
        LambdaQueryWrapper<PrivateChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PrivateChatSession::getUser1Id, user1Id)
                .eq(PrivateChatSession::getUser2Id, user2Id);
        PrivateChatSession session = privateChatSessionMapper.selectOne(queryWrapper);

        // 若不存在会话记录则初始化创建新会话
        if (session == null) {
            session = new PrivateChatSession();
            session.setUser1Id(user1Id);
            session.setUser2Id(user2Id);
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());
            privateChatSessionMapper.insert(session);
        }

        // 组装会话视图并从 Netty 通道管理器获取对方实时在线状态
        PrivateChatSessionVO sessionVO = new PrivateChatSessionVO();
        sessionVO.setSessionId(session.getId());
        UserVO targetUserVO = new UserVO();
        BeanUtils.copyProperties(targetUser, targetUserVO);
        sessionVO.setTargetUser(targetUserVO);
        sessionVO.setIsTargetOnline(chatChannelManager.isUserOnline(targetUserId));
        sessionVO.setLastMessage(session.getLastMessage());
        sessionVO.setLastMessageTime(formatDate(session.getLastMessageTime()));
        sessionVO.setCreateTime(formatDate(session.getCreateTime()));
        return sessionVO;
    }

    /**
     * 持久化私聊消息至 MySQL（异常时降级写入 Redis）并通过 Netty 推送点对点实时消息帧
     *
     * @param loginUser 当前登录用户
     * @param channel   发送方网络通道
     * @param request   客户端上行的私聊消息请求
     */
    @Override
    public void sendPrivateMessage(User loginUser, Channel channel, ChatInboundMessage request) {
        // 校验发送方登录态与消息文本内容长度
        if (loginUser == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息参数错误");
        }
        String content = request.getContent();
        if (StringUtils.isBlank(content) || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空且不能超过2048字符");
        }

        // 解析或自动补全私聊会话与目标接收方用户 id
        Long sessionId = request.getSessionId();
        Long toUserId = request.getToUserId();
        PrivateChatSession session;
        if (sessionId != null && sessionId > 0) {
            session = ensureSessionParticipant(sessionId, loginUser.getId());
            if (toUserId == null) {
                toUserId = session.getUser1Id().equals(loginUser.getId()) ? session.getUser2Id() : session.getUser1Id();
            }
        }
        // 未传入 sessionId 但提供了有效 toUserId 时查询或创建会话
        else if (toUserId != null && toUserId > 0) {
            if (toUserId.equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能向自己发送私聊消息");
            }
            long u1 = Math.min(loginUser.getId(), toUserId);
            long u2 = Math.max(loginUser.getId(), toUserId);
            LambdaQueryWrapper<PrivateChatSession> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PrivateChatSession::getUser1Id, u1)
                    .eq(PrivateChatSession::getUser2Id, u2);
            session = privateChatSessionMapper.selectOne(queryWrapper);
            if (session == null) {
                session = new PrivateChatSession();
                session.setUser1Id(u1);
                session.setUser2Id(u2);
                session.setCreateTime(new Date());
                privateChatSessionMapper.insert(session);
            }
            sessionId = session.getId();
        }
        // 会话 id 与接收方 id 均缺失时抛出异常
        else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少会话 ID 或目标接收者 ID");
        }

        // 组装私聊消息实体并写入 MySQL，若数据库异常则降级保存至 Redis
        Date now = new Date();
        String formattedTime = formatDate(now);
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
            session.setLastMessage(content);
            session.setLastMessageTime(now);
            privateChatSessionMapper.updateById(session);
        } catch (Exception e) {
            log.error("save private chat message to mysql failed, sessionId={}, fromUserId={}, toUserId={}",
                    sessionId, loginUser.getId(), toUserId, e);
            saveFallbackMessage(sessionId, privateChatMessage);
        }

        // 构建下行标准响应帧并向发送方自身通道回推确认帧
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
        if (channel != null && channel.isActive()) {
            sendFrame(channel, response);
        }

        // 检查接收方通道是否在线，在线则实时推送，离线则留存离线消息
        Channel targetChannel = chatChannelManager.getUserChannel(toUserId);
        if (targetChannel != null && targetChannel.isActive()) {
            sendFrame(targetChannel, response);
            log.info("private message delivered online: fromUserId={} -> toUserId={}, sessionId={}",
                    loginUser.getId(), toUserId, sessionId);
        }
        // 接收方离线时记录离线消息留存日志
        else {
            log.info("target user is offline, message saved for offline pickup: toUserId={}, sessionId={}",
                    toUserId, sessionId);
        }
    }

    /**
     * 更新 MySQL 未读消息状态并分页查询私聊历史记录（第 1 页合并 Redis 兜底消息）
     *
     * @param sessionId 会话 id
     * @param pageNum   请求页码
     * @param pageSize  每页条数
     * @param loginUser 当前登录用户
     * @return 历史消息分页结果
     */
    @Override
    public Page<ChatMessageResponse> listSessionMessages(Long sessionId, long pageNum, long pageSize, User loginUser) {
        // 校验会话参数与登录状态，并验证当前用户是否为会话合法参与方
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 ID 参数错误");
        }
        if (loginUser == null || loginUser.getId() <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }
        ensureSessionParticipant(sessionId, loginUser.getId());

        // 将当前会话中发送给当前用户的未读消息批量标记为已读
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

        // 按创建时间倒序分页查询 MySQL 历史私聊消息
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

        // 查询第 1 页时合并 Redis 容灾兜底缓存消息并重排序
        if (pageNum == 1) {
            List<ChatMessageResponse> fallbackList = listFallbackMessages(sessionId);
            if (!fallbackList.isEmpty()) {
                records.addAll(fallbackList);
                records.sort(Comparator.comparing(ChatMessageResponse::getCreateTime,
                        Comparator.nullsLast(String::compareTo)).reversed());
            }
        }

        // 组装并返回分页响应结果
        Page<ChatMessageResponse> responsePage = new Page<>(pageNum, pageSize);
        responsePage.setRecords(records);
        responsePage.setTotal(messagePage.getTotal());
        return responsePage;
    }

    /**
     * 通过数据库查询私聊会话并校验用户是否为会话双方成员
     *
     * @param sessionId 会话 id
     * @param userId    用户 id
     * @return 私聊会话实体
     */
    @Override
    public PrivateChatSession ensureSessionParticipant(Long sessionId, Long userId) {
        // 校验会话 id 与用户 id 参数合法性
        if (sessionId == null || sessionId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询私聊会话并验证用户是否属于会话参与方
        PrivateChatSession session = privateChatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "私聊会话不存在");
        }
        if (!userId.equals(session.getUser1Id()) && !userId.equals(session.getUser2Id())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权访问此私聊会话");
        }
        return session;
    }

    /**
     * 兜底保存未成功写入数据库的私聊消息至 Redis 列表
     *
     * @param sessionId 会话 id
     * @param message   私聊消息实体
     */
    private void saveFallbackMessage(Long sessionId, PrivateChatMessage message) {
        // 转换消息为响应结构并推入 Redis 容灾列表
        try {
            String key = REDIS_FALLBACK_PREFIX + sessionId;
            ChatMessageResponse response = toResponse(message);
            redisTemplate.opsForList().rightPush(key, response);
            redisTemplate.expire(key, FALLBACK_TTL);
        } catch (Exception e) {
            log.error("save private fallback message to redis failed, sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取指定私聊会话在 Redis 中的兜底消息列表
     *
     * @param sessionId 会话 id
     * @return 兜底私聊消息响应列表
     */
    private List<ChatMessageResponse> listFallbackMessages(Long sessionId) {
        // 从 Redis 读取指定会话的容灾兜底消息列表
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

    /**
     * 将私聊消息实体转换为下行响应对象
     *
     * @param item 私聊消息数据库实体
     * @return 聊天消息响应对象
     */
    private ChatMessageResponse toResponse(PrivateChatMessage item) {
        // 映射私聊实体字段至通用消息响应结构
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

    /**
     * 序列化消息对象并通过指定 Netty 通道发送 WebSocket 文本帧
     *
     * @param channel  目标 Netty 通道
     * @param response 待发送的消息响应对象
     */
    private void sendFrame(Channel channel, ChatMessageResponse response) {
        // 序列化响应对象为 JSON 并写入通道
        try {
            channel.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        } catch (JsonProcessingException e) {
            log.error("serialize websocket message failed", e);
        }
    }

    /**
     * 格式化日期对象为标准时间字符串
     *
     * @param date 日期对象
     * @return 格式化后的时间字符串
     */
    private String formatDate(Date date) {
        // 判空并按标准时间格式转换
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }

}

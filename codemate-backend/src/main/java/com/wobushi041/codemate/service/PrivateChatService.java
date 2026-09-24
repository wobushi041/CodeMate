package com.wobushi041.codemate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wobushi041.codemate.chat.ChatInboundMessage;
import com.wobushi041.codemate.chat.ChatMessageResponse;
import com.wobushi041.codemate.model.domain.PrivateChatSession;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.vo.PrivateChatSessionVO;
import io.netty.channel.Channel;

/**
 * 单人私聊服务
 *
 * @author wobushi041
 */
public interface PrivateChatService extends IService<PrivateChatSession> {

    /**
     * 发起或复用单人私聊会话
     *
     * @param targetUserId 目标联系用户 id
     * @param loginUser    当前登录用户
     * @return 私聊会话视图对象
     */
    PrivateChatSessionVO startSession(Long targetUserId, User loginUser);

    /**
     * 发送单人私聊消息
     *
     * @param loginUser 当前登录用户
     * @param channel   发送方网络通道
     * @param request   客户端上行的私聊消息请求
     */
    void sendPrivateMessage(User loginUser, Channel channel, ChatInboundMessage request);

    /**
     * 分页查询指定私聊会话的历史聊天消息
     *
     * @param sessionId 会话 id
     * @param pageNum   请求页码
     * @param pageSize  每页条数
     * @param loginUser 当前登录用户
     * @return 历史聊天消息分页列表
     */
    Page<ChatMessageResponse> listSessionMessages(Long sessionId, long pageNum, long pageSize, User loginUser);

    /**
     * 校验指定用户是否为指定私聊会话的合法参与方
     *
     * @param sessionId 会话 id
     * @param userId    用户 id
     * @return 私聊会话实体
     */
    PrivateChatSession ensureSessionParticipant(Long sessionId, Long userId);

}

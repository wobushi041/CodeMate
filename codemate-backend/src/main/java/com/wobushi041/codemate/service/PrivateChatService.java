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
 * 单人私聊业务服务接口
 *
 * @author wobushi041
 */
public interface PrivateChatService extends IService<PrivateChatSession> {

    /**
     * 发起私聊会话（联系我），开启或复用单人聊天室
     *
     * @param targetUserId 目标联系用户 ID
     * @param loginUser    当前登录用户
     * @return 私聊会话视图对象
     */
    PrivateChatSessionVO startSession(Long targetUserId, User loginUser);

    /**
     * 发送单人私聊消息（支持离线持久化与在线点对点实时推送）
     *
     * @param loginUser 当前登录用户
     * @param channel   发送方网络通道
     * @param request   客户端上行的私聊消息请求
     */
    void sendPrivateMessage(User loginUser, Channel channel, ChatInboundMessage request);

    /**
     * 分页查询指定私聊会话的历史聊天记录
     *
     * @param sessionId 会话 ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param loginUser 当前登录用户
     * @return 历史消息分页结果
     */
    Page<ChatMessageResponse> listSessionMessages(Long sessionId, long pageNum, long pageSize, User loginUser);

    /**
     * 校验指定用户是否为指定会话的合法参与方
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @return 会话实体
     */
    PrivateChatSession ensureSessionParticipant(Long sessionId, Long userId);
}

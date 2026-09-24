package com.wobushi041.codemate.service;

import com.wobushi041.codemate.chat.ChatInboundMessage;
import com.wobushi041.codemate.model.domain.User;
import io.netty.channel.Channel;

/**
 * 队伍聊天室服务
 *
 * @author wobushi041
 */
public interface ChatRoomService {

    /**
     * 用户加入指定队伍的聊天室
     *
     * @param teamId    队伍 id
     * @param loginUser 当前登录用户
     * @param channel   客户端连接通道
     */
    void joinTeamRoom(Long teamId, User loginUser, Channel channel);

    /**
     * 在队伍聊天室内发送并广播聊天消息
     *
     * @param loginUser 当前登录用户
     * @param channel   客户端连接通道
     * @param request   客户端上行的聊天消息请求
     */
    void sendRoomMessage(User loginUser, Channel channel, ChatInboundMessage request);

    /**
     * 校验指定用户是否为指定队伍的有效成员
     *
     * @param teamId 队伍 id
     * @param userId 用户 id
     */
    void ensureTeamMember(Long teamId, Long userId);

}

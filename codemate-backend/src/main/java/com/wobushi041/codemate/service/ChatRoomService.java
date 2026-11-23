package com.wobushi041.codemate.service;

import com.wobushi041.codemate.chat.ChatInboundMessage;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.vo.TeamChatUnreadVO;
import io.netty.channel.Channel;

import java.util.List;

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

    /**
     * 查询当前用户各队伍聊天室的未读消息数
     *
     * @param loginUser 当前登录用户
     * @return 队伍聊天室未读统计列表
     */
    List<TeamChatUnreadVO> listUnreadCounts(User loginUser);

    /**
     * 将指定队伍聊天室标记为已读
     *
     * @param teamId    队伍 id
     * @param loginUser 当前登录用户
     */
    void markTeamRoomRead(Long teamId, User loginUser);

}

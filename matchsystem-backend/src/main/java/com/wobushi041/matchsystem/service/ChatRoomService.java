package com.wobushi041.matchsystem.service;

import com.wobushi041.matchsystem.chat.ChatInboundMessage;
import com.wobushi041.matchsystem.model.domain.User;
import io.netty.channel.Channel;

public interface ChatRoomService {

    void joinTeamRoom(Long teamId, User loginUser, Channel channel);

    void sendRoomMessage(User loginUser, Channel channel, ChatInboundMessage request);

    void ensureTeamMember(Long teamId, Long userId);
}

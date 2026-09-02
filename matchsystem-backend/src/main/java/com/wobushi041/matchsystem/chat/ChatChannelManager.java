package com.wobushi041.matchsystem.chat;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatChannelManager {

    private final Map<Long, Map<Long, Channel>> roomUserChannels = new ConcurrentHashMap<>();

    public void joinRoom(long teamId, long userId, Channel channel) {
        Map<Long, Channel> userChannels = roomUserChannels.computeIfAbsent(teamId, key -> new ConcurrentHashMap<>());
        Channel oldChannel = userChannels.put(userId, channel);
        if (oldChannel != null && oldChannel != channel && oldChannel.isOpen()) {
            oldChannel.close();
        }
    }

    public Channel get(long teamId, long userId) {
        Map<Long, Channel> userChannels = roomUserChannels.get(teamId);
        if (userChannels == null) {
            return null;
        }
        Channel channel = userChannels.get(userId);
        if (channel == null || !channel.isActive()) {
            return null;
        }
        return channel;
    }

    public Map<Long, Channel> getRoomChannels(long teamId) {
        Map<Long, Channel> userChannels = roomUserChannels.get(teamId);
        if (userChannels == null || userChannels.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Channel> activeChannels = new HashMap<>();
        userChannels.forEach((userId, channel) -> {
            if (channel != null && channel.isActive()) {
                activeChannels.put(userId, channel);
            }
        });
        return activeChannels;
    }

    public boolean isOnline(long teamId, long userId) {
        return get(teamId, userId) != null;
    }

    public void leaveRoom(long teamId, long userId, Channel channel) {
        Map<Long, Channel> userChannels = roomUserChannels.get(teamId);
        if (userChannels == null) {
            return;
        }
        userChannels.remove(userId, channel);
        if (userChannels.isEmpty()) {
            roomUserChannels.remove(teamId, userChannels);
        }
    }
}

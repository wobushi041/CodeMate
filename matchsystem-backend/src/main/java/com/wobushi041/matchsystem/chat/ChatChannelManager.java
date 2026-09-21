package com.wobushi041.matchsystem.chat;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天室通道会话管理器
 *
 * 负责在内存中统一维护各队伍聊天室与在线客户端 Netty Channel 的映射关系，
 * 提供用户加入房间、离开房间、连接检索、在线状态判断以及多端旧连接互踢等管理能力。
 *
 * 拓扑结构：
 * 队伍 ID (teamId)
 *    ├── 用户 1 (userId_1) -> 客户端 Channel_1
 *    ├── 用户 2 (userId_2) -> 客户端 Channel_2
 *    └── ...
 *
 * @author 041
 */
@Component
public class ChatChannelManager {

    /**
     * 维护队伍房间与其内部所有用户在线 Channel 的二级并发映射表
     * 格式: teamId -> (userId -> Channel)
     */
    private final Map<Long, Map<Long, Channel>> roomUserChannels = new ConcurrentHashMap<>();

    /**
     * 用户加入指定队伍房间
     *
     * 若该用户在该队伍中已存在旧的连接，会主动关闭旧连接以保障单一会话活跃。
     *
     * @param teamId  队伍 ID
     * @param userId  用户 ID
     * @param channel 客户端网络通道
     */
    public void joinRoom(long teamId, long userId, Channel channel) {
        Map<Long, Channel> userChannels = roomUserChannels.computeIfAbsent(teamId, key -> new ConcurrentHashMap<>());
        Channel oldChannel = userChannels.put(userId, channel);
        if (oldChannel != null && oldChannel != channel && oldChannel.isOpen()) {
            oldChannel.close();
        }
    }

    /**
     * 获取指定队伍中指定用户的活跃 Channel
     *
     * @param teamId 队伍 ID
     * @param userId 用户 ID
     * @return 活跃的 Channel 实例；若不存在或已非活跃则返回 null
     */
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

    /**
     * 获取指定队伍房间中所有活跃的 Channel 映射
     *
     * @param teamId 队伍 ID
     * @return 活跃 Channel 映射表（userId -> Channel）
     */
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

    /**
     * 判断指定用户在指定队伍中是否在线
     *
     * @param teamId 队伍 ID
     * @param userId 用户 ID
     * @return true 表示在线，false 表示离线
     */
    public boolean isOnline(long teamId, long userId) {
        return get(teamId, userId) != null;
    }

    /**
     * 用户离开指定队伍房间
     *
     * 从房间用户列表中移除指定 Channel；若房间内已无任何在线成员，则清理并释放房间 Map 容器。
     *
     * @param teamId  队伍 ID
     * @param userId  用户 ID
     * @param channel 待移除的客户端网络通道
     */
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

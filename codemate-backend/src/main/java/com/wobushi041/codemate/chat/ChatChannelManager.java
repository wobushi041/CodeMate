package com.wobushi041.codemate.chat;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天室通道会话管理器
 *
 * @author wobushi041
 */
@Component
public class ChatChannelManager {

    /**
     * 维护队伍房间与其内部所有用户在线 Channel 的二级并发映射表（teamId -> (userId -> Channel)）
     */
    private final Map<Long, Map<Long, Channel>> roomUserChannels = new ConcurrentHashMap<>();

    /**
     * 全局用户在线 Channel 映射表（userId -> Channel）
     */
    private final Map<Long, Channel> userChannels = new ConcurrentHashMap<>();

    /**
     * 将用户加入指定队伍房间并维护单一活跃连接
     *
     * @param teamId  队伍 ID
     * @param userId  用户 ID
     * @param channel 客户端网络通道
     */
    public void joinRoom(long teamId, long userId, Channel channel) {
        // 获取或初始化队伍房间的用户通道映射表
        Map<Long, Channel> userChannels = roomUserChannels.computeIfAbsent(teamId, key -> new ConcurrentHashMap<>());

        // 绑定新通道，若存在未关闭的旧通道则主动关闭以互踢旧连接
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
        // 获取队伍房间映射表并校验是否存在
        Map<Long, Channel> userChannels = roomUserChannels.get(teamId);
        if (userChannels == null) {
            return null;
        }

        // 获取指定用户的通道并校验活跃状态
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
        // 查询队伍房间内的用户通道表，为空则返回空集合
        Map<Long, Channel> userChannels = roomUserChannels.get(teamId);
        if (userChannels == null || userChannels.isEmpty()) {
            return Collections.emptyMap();
        }

        // 筛选出所有处于活跃状态的客户端通道
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
        // 通过是否存在活跃通道判断用户房间在线状态
        return get(teamId, userId) != null;
    }

    /**
     * 将用户移出指定队伍房间并在房间为空时释放容器
     *
     * @param teamId  队伍 ID
     * @param userId  用户 ID
     * @param channel 待移除的客户端网络通道
     */
    public void leaveRoom(long teamId, long userId, Channel channel) {
        // 获取队伍房间的用户通道映射表
        Map<Long, Channel> userChannels = roomUserChannels.get(teamId);
        if (userChannels == null) {
            return;
        }

        // 移除指定通道，若房间已无在线成员则回收房间映射表
        userChannels.remove(userId, channel);
        if (userChannels.isEmpty()) {
            roomUserChannels.remove(teamId, userChannels);
        }
    }

    /**
     * 注册用户全局活跃 Channel 并关闭同账号旧连接
     *
     * @param userId  用户 ID
     * @param channel 客户端网络通道
     */
    public void registerUserChannel(Long userId, Channel channel) {
        // 参数判空校验
        if (userId == null || channel == null) {
            return;
        }

        // 写入全局通道表，若存在旧连接则主动关闭
        Channel oldChannel = userChannels.put(userId, channel);
        if (oldChannel != null && oldChannel != channel && oldChannel.isOpen()) {
            oldChannel.close();
        }
    }

    /**
     * 根据用户 ID 获取其全局活跃 Channel
     *
     * @param userId 用户 ID
     * @return 活跃的 Channel 实例；若不在线或非活跃则返回 null
     */
    public Channel getUserChannel(Long userId) {
        // 参数判空校验
        if (userId == null) {
            return null;
        }

        // 获取全局通道并校验活跃状态
        Channel channel = userChannels.get(userId);
        if (channel == null || !channel.isActive()) {
            return null;
        }
        return channel;
    }

    /**
     * 移除用户的全局活跃 Channel
     *
     * @param userId  用户 ID
     * @param channel 待移除的客户端网络通道
     */
    public void removeUserChannel(Long userId, Channel channel) {
        // 参数判空校验
        if (userId == null || channel == null) {
            return;
        }

        // 从全局通道表中精确移除目标通道
        userChannels.remove(userId, channel);
    }

    /**
     * 检测指定用户是否在全局在线
     *
     * @param userId 用户 ID
     * @return true 表示在线，false 表示离线
     */
    public boolean isUserOnline(Long userId) {
        // 通过是否存在全局活跃通道判断用户在线状态
        return getUserChannel(userId) != null;
    }

}

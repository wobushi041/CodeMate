package com.wobushi041.matchsystem.chat;

import com.wobushi041.matchsystem.model.domain.User;
import io.netty.util.AttributeKey;

/**
 * Netty Channel 属性常量类
 *
 * 提供 Netty AttributeKey 常量定义，用于在 Channel 生命周期中绑定和存取会话上下文数据
 * （如已登录用户信息、当前所在队伍 ID）。
 *
 * @author 041
 */
public final class ChatAttributes {

    /**
     * 绑定在 Channel 上的当前登录用户信息
     */
    public static final AttributeKey<User> LOGIN_USER = AttributeKey.valueOf("loginUser");

    /**
     * 绑定在 Channel 上的当前用户加入的队伍 ID
     */
    public static final AttributeKey<Long> TEAM_ID = AttributeKey.valueOf("teamId");

    /**
     * 绑定在 Channel 上的当前用户进入的单人私聊会话 ID
     */
    public static final AttributeKey<Long> SESSION_ID = AttributeKey.valueOf("sessionId");

    /**
     * 私有构造函数，防止工具类被实例化
     */
    private ChatAttributes() {
    }
}

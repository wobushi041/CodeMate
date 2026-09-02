package com.wobushi041.matchsystem.chat;

import com.wobushi041.matchsystem.model.domain.User;
import io.netty.util.AttributeKey;

public final class ChatAttributes {
//AttributeKey是netty提供的，用于在channel中存储自定义属性，
// loginUser 是 AttributeKey 的名称（字符串）。
//LOGIN_USER 是 AttributeKey 的实例（键）。
//User 对象是通过键存储的值（Value）。
    public static final AttributeKey<User> LOGIN_USER = AttributeKey.valueOf("loginUser");
    public static final AttributeKey<Long> TEAM_ID = AttributeKey.valueOf("teamId");

    private ChatAttributes() {
    }
}

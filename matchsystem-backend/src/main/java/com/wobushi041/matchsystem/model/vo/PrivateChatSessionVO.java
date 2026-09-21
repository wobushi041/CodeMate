package com.wobushi041.matchsystem.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 私聊会话视图对象
 *
 * @author wobushi041
 */
@Data
public class PrivateChatSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID (sessionId)
     */
    private Long sessionId;

    /**
     * 目标联系用户脱敏资料
     */
    private UserVO targetUser;

    /**
     * 目标用户当前是否在线（基于 Netty Channel 活跃状态实时判定）
     */
    private Boolean isTargetOnline;

    /**
     * 最后一条消息内容摘要
     */
    private String lastMessage;

    /**
     * 最后消息发送时间
     */
    private String lastMessageTime;

    /**
     * 会话创建时间
     */
    private String createTime;
}

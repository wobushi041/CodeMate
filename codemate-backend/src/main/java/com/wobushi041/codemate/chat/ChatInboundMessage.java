package com.wobushi041.codemate.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户端上行 WebSocket 消息载荷
 *
 * @author wobushi041
 */
@Data
public class ChatInboundMessage implements Serializable {

    /**
     * 消息请求操作类型（JOIN / CHAT / PRIVATE_JOIN / PRIVATE_CHAT）
     */
    private String type;

    /**
     * 目标队伍 ID（队伍群聊时使用）
     */
    private Long teamId;

    /**
     * 私聊会话 ID（单聊时使用）
     */
    private Long sessionId;

    /**
     * 目标接收者用户 ID（单聊时使用）
     */
    private Long toUserId;

    /**
     * 客户端生成的消息唯一追踪 ID，用于消息去重、ACK 回执与乐观 UI 渲染
     */
    private String clientMessageId;

    /**
     * 聊天消息正文内容（最大长度为 2048 个字符）
     */
    private String content;

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}

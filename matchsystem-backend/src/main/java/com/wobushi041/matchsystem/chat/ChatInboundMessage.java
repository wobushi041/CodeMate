package com.wobushi041.matchsystem.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 客户端上行 WebSocket 消息载荷
 *
 * 封装前端或客户端通过 WebSocket 长连接发送至服务端的各类请求数据。
 *
 * @author 硫酸铜
 */
@Data
public class ChatInboundMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息请求操作类型
     * - "JOIN": 请求加入指定队伍的聊天室
     * - "CHAT": 在当前队伍聊天室中发送文本消息
     * - "PRIVATE_JOIN": 进入单人聊天室/绑定会话
     * - "PRIVATE_CHAT": 发送单人私聊消息
     */
    private String type;

    /**
     * 目标队伍 ID (队伍群聊时使用)
     */
    private Long teamId;

    /**
     * 私聊会话 ID (单聊时使用)
     */
    private Long sessionId;

    /**
     * 目标接收者用户 ID (单聊时使用)
     */
    private Long toUserId;

    /**
     * 客户端生成的消息唯一追踪 ID，用于消息去重、ACK 回执与乐观 UI 渲染
     */
    private String clientMessageId;

    /**
     * 聊天消息正文内容（当 type 为 CHAT / PRIVATE_CHAT 时必填，长度上限为 2048 字符）
     */
    private String content;
}

package com.wobushi041.matchsystem.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务端下行 WebSocket 响应消息载荷
 *
 * 用于服务端向客户端推送广播聊天消息、加入房间回执以及错误提示信息等。
 *
 * @author 硫酸铜
 */
@Data
public class ChatMessageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应消息类型
     * - "JOINED": 加入队伍聊天室成功确认
     * - "CHAT": 队伍聊天消息广播
     * - "PRIVATE_JOINED": 进入单聊会话成功确认
     * - "PRIVATE_CHAT": 单人私聊消息推送/回执
     * - "ERROR": 异常或错误提示通知
     */
    private String type;

    /**
     * 所属队伍 ID (队伍群聊时使用)
     */
    private Long teamId;

    /**
     * 私聊会话 ID (单聊时使用)
     */
    private Long sessionId;

    /**
     * 消息接收方用户 ID (单聊时使用)
     */
    private Long toUserId;

    /**
     * 消息持久化存储生成的唯一主键 ID（未持久化时可能为 null）
     */
    private Long messageId;

    /**
     * 客户端发送时附带的追踪 ID，用于前端将乐观更新的消息与服务端广播做精准关联
     */
    private String clientMessageId;

    /**
     * 消息发送方用户 ID
     */
    private Long fromUserId;

    /**
     * 消息发送方用户昵称/用户名
     */
    private String fromUsername;

    /**
     * 聊天消息正文内容
     */
    private String content;

    /**
     * 系统提示或错误描述信息（主要用于 ERROR 类型）
     */
    private String message;

    /**
     * 消息创建时间，格式为 yyyy-MM-dd HH:mm:ss
     */
    private String createTime;

    /**
     * 快速构建错误响应对象
     *
     * @param message 错误描述信息
     * @return 错误响应实例
     */
    public static ChatMessageResponse error(String message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("ERROR");
        response.setMessage(message);
        return response;
    }

    /**
     * 快速构建成功加入房间的确认响应对象
     *
     * @param teamId 成功加入的队伍 ID
     * @return 进房成功响应实例
     */
    public static ChatMessageResponse joined(Long teamId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("JOINED");
        response.setTeamId(teamId);
        return response;
    }

    /**
     * 快速构建进入单聊会话的确认响应对象
     *
     * @param sessionId 成功进入的会话 ID
     * @return 进房成功响应实例
     */
    public static ChatMessageResponse privateJoined(Long sessionId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("PRIVATE_JOINED");
        response.setSessionId(sessionId);
        return response;
    }
}

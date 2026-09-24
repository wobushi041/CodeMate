package com.wobushi041.codemate.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务端下行 WebSocket 响应消息载荷
 *
 * @author wobushi041
 */
@Data
public class ChatMessageResponse implements Serializable {

    /**
     * 响应消息类型（JOINED / CHAT / PRIVATE_JOINED / PRIVATE_CHAT / ERROR）
     */
    private String type;

    /**
     * 所属队伍 ID（队伍群聊时使用）
     */
    private Long teamId;

    /**
     * 私聊会话 ID（单聊时使用）
     */
    private Long sessionId;

    /**
     * 消息接收方用户 ID（单聊时使用）
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
     * 消息发送方用户昵称或用户名
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
     * 消息创建时间（格式为 yyyy-MM-dd HH:mm:ss）
     */
    private String createTime;

    /**
     * 快速构建错误响应对象
     *
     * @param message 错误描述信息
     * @return 错误响应实例
     */
    public static ChatMessageResponse error(String message) {
        // 构造 ERROR 类型的消息响应对象
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("ERROR");
        response.setMessage(message);
        return response;
    }

    /**
     * 快速构建成功加入队伍房间的确认响应对象
     *
     * @param teamId 成功加入的队伍 ID
     * @return 进房成功响应实例
     */
    public static ChatMessageResponse joined(Long teamId) {
        // 构造 JOINED 类型的队伍进房确认响应对象
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
        // 构造 PRIVATE_JOINED 类型的单聊会话确认响应对象
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("PRIVATE_JOINED");
        response.setSessionId(sessionId);
        return response;
    }

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}

package com.wobushi041.codemate.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 私聊未读消息统计视图
 *
 * @author wobushi041
 */
@Data
public class PrivateChatUnreadVO implements Serializable {

    /**
     * 私聊会话 id
     */
    private Long sessionId;

    /**
     * 当前用户在该会话中的聊天对象 id
     */
    private Long targetUserId;

    /**
     * 未读消息数量
     */
    private Long unreadCount;

    private static final long serialVersionUID = 1L;
}

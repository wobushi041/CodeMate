package com.wobushi041.codemate.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 队伍聊天室未读消息统计视图
 *
 * @author wobushi041
 */
@Data
public class TeamChatUnreadVO implements Serializable {

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 未读消息数量
     */
    private Long unreadCount;

    private static final long serialVersionUID = 1L;
}

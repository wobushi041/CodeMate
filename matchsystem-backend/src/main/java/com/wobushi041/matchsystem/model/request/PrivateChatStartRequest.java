package com.wobushi041.matchsystem.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 发起私聊请求对象
 *
 * @author wobushi041
 */
@Data
public class PrivateChatStartRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标联系用户 ID
     */
    private Long targetUserId;
}

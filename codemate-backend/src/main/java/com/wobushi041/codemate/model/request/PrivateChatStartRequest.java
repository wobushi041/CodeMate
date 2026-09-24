package com.wobushi041.codemate.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 发起私聊请求对象
 *
 * @author wobushi041
 */
@Data
public class PrivateChatStartRequest implements Serializable {

    /**
     * 目标联系用户 id
     */
    private Long targetUserId;

    /// 序列化字段 ///
    private static final long serialVersionUID = 1L;

}

package com.wobushi041.codemate.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求
 *
 * @author wobushi041
 */
@Data
public class TeamJoinRequest implements Serializable {

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 队伍密码
     */
    private String password;

    /// 序列化字段 ///
    private static final long serialVersionUID = -1460976109170238609L;

}

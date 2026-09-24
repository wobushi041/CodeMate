package com.wobushi041.codemate.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户退出队伍请求
 *
 * @author wobushi041
 */
@Data
public class TeamQuitRequest implements Serializable {

    /**
     * 队伍 id
     */
    private Long teamId;

    /// 序列化字段 ///
    private static final long serialVersionUID = 6395006031284178770L;

}

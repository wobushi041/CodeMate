package com.wobushi041.codemate.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍更新请求
 *
 * @author wobushi041
 */
@Data
public class TeamUpdateRequest implements Serializable {

    /**
     * 队伍 id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 队伍状态 0 - 公开 1 - 私有 2 - 加密
     */
    private Integer status;

    /**
     * 队伍密码
     */
    private String password;

    /// 序列化字段 ///
    private static final long serialVersionUID = -8351268843240931365L;

}
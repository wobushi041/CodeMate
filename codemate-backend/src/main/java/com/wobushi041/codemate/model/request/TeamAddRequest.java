package com.wobushi041.codemate.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建队伍请求
 *
 * @author wobushi041
 */
@Data
public class TeamAddRequest implements Serializable {

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人用户 id
     */
    private Long userId;

    /**
     * 队伍状态 0 - 公开 1 - 私有 2 - 加密
     */
    private Integer status;

    /**
     * 队伍密码
     */
    private String password;

    /// 序列化字段 ///
    private static final long serialVersionUID = 6191190645590093049L;

}

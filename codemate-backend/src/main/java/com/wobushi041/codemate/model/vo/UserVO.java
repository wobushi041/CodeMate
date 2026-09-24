package com.wobushi041.codemate.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户脱敏信息视图对象
 *
 * @author wobushi041
 */
@Data
public class UserVO implements Serializable {

    /**
     * 用户 id
     */
    private long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 登录账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 标签列表 JSON
     */
    private String tags;

    /**
     * 用户状态 0 - 正常
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户角色 0 - 普通用户 1 - 管理员
     */
    private Integer userRole;

    /**
     * 星球编号
     */
    private String planetCode;

    /// 序列化字段 ///
    private static final long serialVersionUID = -4125063705679526933L;

}

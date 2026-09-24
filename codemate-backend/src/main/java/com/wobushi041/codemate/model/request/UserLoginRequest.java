package com.wobushi041.codemate.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author wobushi041
 */
@Data
public class UserLoginRequest implements Serializable {

    /**
     * 用户账号
     */
    @Size(min = 4, message = "账号不能少于4位")
    @NotBlank(message = "账号不能为空")
    private String userAccount;

    /**
     * 用户密码
     */
    @Size(min = 8, message = "密码不能少于8位")
    @NotBlank(message = "密码不能为空")
    private String userPassword;

    /// 序列化字段 ///
    private static final long serialVersionUID = 3191241716373120793L;

}

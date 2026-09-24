package com.wobushi041.codemate.model.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author wobushi041
 */
@Data
@NoArgsConstructor
public class UserRegisterRequest implements Serializable {

    /**
     * 用户账号
     */
    @NotNull(message = "账号不能为空")
    @Size(min = 4, message = "账号不能少于4位")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotNull(message = "密码不能为空")
    @Size(min = 8, message = "密码不能少于8位")
    private String userPassword;

    /**
     * 校验密码
     */
    @NotNull(message = "验证密码不能为空")
    @Size(min = 8, message = "校验密码不能少于8位")
    private String checkPassword;

    /**
     * 星球编号
     */
    @NotNull(message = "星球编号不能为空")
    @Size(max = 5, message = "星球编号过长")
    private String planetCode;

    /// 序列化字段 ///
    private static final long serialVersionUID = 3191241716373120793L;

}

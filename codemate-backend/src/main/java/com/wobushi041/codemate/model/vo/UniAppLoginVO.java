package com.wobushi041.codemate.model.vo;

import com.wobushi041.codemate.model.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * uni-app 登录响应
 *
 * @author wobushi041
 */
@Data
@AllArgsConstructor
public class UniAppLoginVO implements Serializable {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 令牌类型，请求时用于 Authorization 请求头前缀
     */
    private String tokenType;

    /**
     * 脱敏后的登录用户信息
     */
    private User userInfo;

    private static final long serialVersionUID = 1L;
}

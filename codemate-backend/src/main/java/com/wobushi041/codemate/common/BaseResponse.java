package com.wobushi041.codemate.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应结果封装类
 *
 * @author wobushi041
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应数据载荷
     */
    private T data;

    /**
     * 响应状态消息
     */
    private String message;

    /**
     * 响应详细描述
     */
    private String description;

    /**
     * 全参构造通用响应结果
     *
     * @param code        响应状态码
     * @param data        响应数据载荷
     * @param message     响应状态消息
     * @param description 响应详细描述
     */
    public BaseResponse(int code, T data, String message, String description) {
        // 初始化响应状态码、数据载荷、状态消息与详细描述
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    /**
     * 根据状态码、数据载荷与状态消息构造通用响应结果
     *
     * @param code    响应状态码
     * @param data    响应数据载荷
     * @param message 响应状态消息
     */
    public BaseResponse(int code, T data, String message) {
        // 默认详细描述置为空字符串
        this(code, data, message, "");
    }

    /**
     * 根据状态码与数据载荷构造通用响应结果
     *
     * @param code 响应状态码
     * @param data 响应数据载荷
     */
    public BaseResponse(int code, T data) {
        // 默认状态消息与详细描述置为空字符串
        this(code, data, "", "");
    }

    /**
     * 根据错误码枚举构造通用响应结果
     *
     * @param errorCode 错误码枚举
     */
    public BaseResponse(ErrorCode errorCode) {
        // 提取错误码枚举中的状态码、消息与描述进行初始化
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }

}

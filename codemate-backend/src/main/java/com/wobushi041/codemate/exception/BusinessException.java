package com.wobushi041.codemate.exception;

import com.wobushi041.codemate.common.ErrorCode;

/**
 * 自定义业务异常类
 *
 * @author wobushi041
 */
public class BusinessException extends RuntimeException {

    /**
     * 业务异常状态码
     */
    private final int code;

    /**
     * 业务异常详细描述
     */
    private final String description;

    /**
     * 根据自定义消息、状态码与详细描述构造业务异常
     *
     * @param message     异常消息摘要
     * @param code        业务异常状态码
     * @param description 业务异常详细描述
     */
    public BusinessException(String message, int code, String description) {
        // 调用父类构造器设置消息摘要，并初始化状态码与详细描述
        super(message);
        this.code = code;
        this.description = description;
    }

    /**
     * 根据错误码枚举构造业务异常
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        // 从错误码枚举提取消息摘要、状态码与默认描述
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /**
     * 根据错误码枚举与自定义详细描述构造业务异常
     *
     * @param errorCode   错误码枚举
     * @param description 自定义异常详细描述
     */
    public BusinessException(ErrorCode errorCode, String description) {
        // 从错误码枚举提取消息摘要与状态码，并使用自定义详细描述
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    /**
     * 获取业务异常状态码
     *
     * @return 业务异常状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取业务异常详细描述
     *
     * @return 业务异常详细描述
     */
    public String getDescription() {
        return description;
    }

}

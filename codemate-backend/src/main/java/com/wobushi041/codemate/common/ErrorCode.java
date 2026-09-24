package com.wobushi041.codemate.common;

/**
 * 全局错误码枚举
 *
 * @author wobushi041
 */
public enum ErrorCode {

    // 成功状态码
    /**
     * 操作成功
     */
    SUCCESS(0, "ok", ""),

    // 客户端请求与权限错误码
    /**
     * 请求参数错误
     */
    PARAMS_ERROR(40000, "请求参数错误", ""),

    /**
     * 请求数据为空
     */
    NULL_ERROR(40001, "请求数据为空", ""),

    /**
     * 未登录
     */
    NOT_LOGIN(40100, "未登录", ""),

    /**
     * 无权限
     */
    NO_AUTH(40101, "无权限", ""),

    /**
     * 禁止操作
     */
    FORBIDDEN(40301, "禁止操作", ""),

    // 服务端错误码
    /**
     * 系统内部异常
     */
    SYSTEM_ERROR(50000, "系统内部异常", "");

    /**
     * 错误状态码
     */
    private final int code;

    /**
     * 状态码信息
     */
    private final String message;

    /**
     * 状态码描述（详情）
     */
    private final String description;

    /**
     * 构造错误码枚举实例
     *
     * @param code        错误状态码
     * @param message     状态码信息
     * @param description 状态码详细描述
     */
    ErrorCode(int code, String message, String description) {
        // 初始化错误码枚举属性
        this.code = code;
        this.message = message;
        this.description = description;
    }

    /**
     * 获取错误状态码
     *
     * @return 错误状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态码信息
     *
     * @return 状态码信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取状态码详细描述
     *
     * @return 状态码详细描述
     */
    public String getDescription() {
        return description;
    }

}

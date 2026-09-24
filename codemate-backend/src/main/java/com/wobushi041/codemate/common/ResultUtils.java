package com.wobushi041.codemate.common;

/**
 * 统一响应结果构造工具类
 *
 * @author wobushi041
 */
public class ResultUtils {

    /**
     * 构造成功响应体
     *
     * @param data 响应数据载荷
     * @param <T>  响应数据泛型类型
     * @return 成功响应体封装
     */
    public static <T> BaseResponse<T> success(T data) {
        // 封装状态码为 0 的成功响应结果
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 根据错误码枚举构造失败响应体
     *
     * @param errorCode 错误码枚举
     * @return 失败响应体封装
     */
    public static BaseResponse error(ErrorCode errorCode) {
        // 使用错误码枚举默认属性封装失败响应结果
        return new BaseResponse<>(errorCode);
    }

    /**
     * 根据自定义状态码、错误消息与详细描述构造失败响应体
     *
     * @param code        自定义错误状态码
     * @param message     错误消息摘要
     * @param description 错误详细描述
     * @return 失败响应体封装
     */
    public static BaseResponse error(int code, String message, String description) {
        // 使用自定义状态码、消息与详细描述封装失败响应结果
        return new BaseResponse(code, null, message, description);
    }

    /**
     * 根据错误码枚举、自定义消息与详细描述构造失败响应体
     *
     * @param errorCode   错误码枚举
     * @param message     自定义错误消息
     * @param description 错误详细描述
     * @return 失败响应体封装
     */
    public static BaseResponse error(ErrorCode errorCode, String message, String description) {
        // 复用错误码枚举的状态码并结合自定义消息与描述封装响应
        return new BaseResponse(errorCode.getCode(), null, message, description);
    }

    /**
     * 根据错误码枚举与详细描述构造失败响应体
     *
     * @param errorCode   错误码枚举
     * @param description 错误详细描述
     * @return 失败响应体封装
     */
    public static BaseResponse error(ErrorCode errorCode, String description) {
        // 复用错误码枚举的状态码与默认消息并结合详细描述封装响应
        return new BaseResponse(errorCode.getCode(), errorCode.getMessage(), description);
    }

}

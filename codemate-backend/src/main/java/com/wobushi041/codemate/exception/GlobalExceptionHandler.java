package com.wobushi041.codemate.exception;

import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author wobushi041
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获并处理请求体参数校验异常
     *
     * @param e 方法参数校验异常对象
     * @return 包含字段校验错误信息的失败响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        // 记录参数校验异常日志
        log.error("methodArgumentNotValidException", e);

        // 提取首个非空字段校验错误提示并返回失败响应
        String description = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(message -> message != null && !message.isEmpty())
                .findFirst()
                .orElse(ErrorCode.PARAMS_ERROR.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), ErrorCode.PARAMS_ERROR.getMessage(), description);
    }

    /**
     * 捕获并处理路径变量或请求参数约束违反异常
     *
     * @param e 约束违反异常对象
     * @return 包含约束错误信息的失败响应体
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        // 记录约束违反异常日志
        log.error("constraintViolationException", e);

        // 拼接所有非空约束违反错误提示并返回失败响应
        String description = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(message -> message != null && !message.isEmpty())
                .collect(Collectors.joining(", "));
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), ErrorCode.PARAMS_ERROR.getMessage(), description);
    }

    /**
     * 捕获并处理自定义业务异常
     *
     * @param e 自定义业务异常对象
     * @return 包含业务错误码与详细描述的失败响应体
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        // 记录业务异常日志并返回对应错误响应
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    /**
     * 捕获并处理系统未知运行时异常
     *
     * @param e 运行时异常对象
     * @return 系统内部异常响应体
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        // 记录系统运行时异常日志并返回兜底错误响应
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "");
    }

}

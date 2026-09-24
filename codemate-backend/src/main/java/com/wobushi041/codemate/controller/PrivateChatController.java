package com.wobushi041.codemate.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.chat.ChatMessageResponse;
import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.common.ResultUtils;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.request.PrivateChatStartRequest;
import com.wobushi041.codemate.model.vo.PrivateChatSessionVO;
import com.wobushi041.codemate.service.PrivateChatService;
import com.wobushi041.codemate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;

/**
 * 单人私聊控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/chat/private")
@Validated
@Slf4j
public class PrivateChatController {

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入私聊会话服务依赖
     */
    @Resource
    private PrivateChatService privateChatService;

    /**
     * 发起私聊会话接口，开启或复用单人私聊房间
     *
     * @param startRequest 发起私聊请求参数（包含目标用户 id）
     * @param request      HTTP 请求对象
     * @return 私聊会话详情视图对象
     */
    @PostMapping("/start")
    public BaseResponse<PrivateChatSessionVO> startSession(
            @RequestBody @Validated PrivateChatStartRequest startRequest,
            HttpServletRequest request) {
        // 校验目标用户参数合法性
        if (startRequest == null || startRequest.getTargetUserId() == null || startRequest.getTargetUserId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户参数错误");
        }

        // 获取当前登录用户并创建或复用私聊会话
        User loginUser = userService.getLoginUserFromRequest(request);
        PrivateChatSessionVO sessionVO = privateChatService.startSession(startRequest.getTargetUserId(), loginUser);
        return ResultUtils.success(sessionVO);
    }

    /**
     * 分页查询指定私聊会话的历史聊天消息记录接口
     *
     * @param sessionId 私聊会话 id
     * @param pageNum   分页页码（默认为 1）
     * @param pageSize  每页记录数（默认为 20）
     * @param request   HTTP 请求对象
     * @return 私聊历史消息分页结果
     */
    @GetMapping("/messages")
    public BaseResponse<Page<ChatMessageResponse>> listSessionMessages(
            @RequestParam("sessionId") @Min(1) Long sessionId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            HttpServletRequest request) {
        // 获取当前登录用户并分页查询私聊历史消息
        User loginUser = userService.getLoginUserFromRequest(request);
        Page<ChatMessageResponse> messagePage = privateChatService.listSessionMessages(sessionId, pageNum, pageSize, loginUser);
        return ResultUtils.success(messagePage);
    }

}

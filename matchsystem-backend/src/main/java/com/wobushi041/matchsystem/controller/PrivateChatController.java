package com.wobushi041.matchsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.chat.ChatMessageResponse;
import com.wobushi041.matchsystem.common.BaseResponse;
import com.wobushi041.matchsystem.common.ErrorCode;
import com.wobushi041.matchsystem.common.ResultUtils;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.model.request.PrivateChatStartRequest;
import com.wobushi041.matchsystem.model.vo.PrivateChatSessionVO;
import com.wobushi041.matchsystem.service.PrivateChatService;
import com.wobushi041.matchsystem.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单人私聊控制层接口
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/chat/private")
@Validated
@Slf4j
public class PrivateChatController {

    @Resource
    private UserService userService;

    @Resource
    private PrivateChatService privateChatService;

    /**
     * 发起私聊会话（联系我），开启或复用单人聊天室
     *
     * @param startRequest 发起私聊请求（包含目标用户 ID）
     * @param request      HTTP 请求对象
     * @return 会话详情（含 sessionId、目标脱敏资料、在线状态等）
     */
    @PostMapping("/start")
    public BaseResponse<PrivateChatSessionVO> startSession(
            @RequestBody @Validated PrivateChatStartRequest startRequest,
            HttpServletRequest request) {
        if (startRequest == null || startRequest.getTargetUserId() == null || startRequest.getTargetUserId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户参数错误");
        }
        User loginUser = userService.getLoginUserFromRequest(request);
        PrivateChatSessionVO sessionVO = privateChatService.startSession(startRequest.getTargetUserId(), loginUser);
        return ResultUtils.success(sessionVO);
    }

    /**
     * 分页查询指定私聊会话的历史聊天记录
     *
     * @param sessionId 会话 ID
     * @param pageNum   页码 (默认 1)
     * @param pageSize  每页大小 (默认 20)
     * @param request   HTTP 请求对象
     * @return 历史消息分页结果
     */
    @GetMapping("/messages")
    public BaseResponse<Page<ChatMessageResponse>> listSessionMessages(
            @RequestParam("sessionId") @Min(1) Long sessionId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUserFromRequest(request);
        Page<ChatMessageResponse> messagePage = privateChatService.listSessionMessages(sessionId, pageNum, pageSize, loginUser);
        return ResultUtils.success(messagePage);
    }
}

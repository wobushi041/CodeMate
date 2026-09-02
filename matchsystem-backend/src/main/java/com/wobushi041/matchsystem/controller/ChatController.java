package com.wobushi041.matchsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.chat.ChatMessageResponse;
import com.wobushi041.matchsystem.service.ChatMessageService;
import com.wobushi041.matchsystem.service.ChatRoomService;
import com.wobushi041.matchsystem.common.BaseResponse;
import com.wobushi041.matchsystem.common.ResultUtils;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@Validated
public class ChatController {

    @Resource
    private UserService userService;

    @Resource
    private ChatRoomService chatRoomService;

    @Resource
    private ChatMessageService chatMessageService;

/**
 * 获取指定team的聊天消息列表
 * 该接口支持分页查询，并验证用户是否为团队成员
 *
 * @param teamId 团队ID，必须大于等于1
 * @param pageNum 页码，默认为1
 * @param pageSize 每页大小，默认为20
 * @param request HTTP请求对象，用于获取当前登录用户信息
 * @return 返回聊天消息的分页结果
 */
    @GetMapping("/teams/{teamId}/messages")
    public BaseResponse<Page<ChatMessageResponse>> listTeamMessages(@PathVariable("teamId") @Min(1) Long teamId, // 团队ID路径变量，使用@Min注解确保值大于等于1
                                                                    @RequestParam(value = "pageNum", defaultValue = "1") long pageNum, // 页码参数，默认值为1
                                                                    @RequestParam(value = "pageSize", defaultValue = "20") long pageSize, // 每页大小参数，默认值为20
                                                                    HttpServletRequest request) { // HTTP请求对象
        User loginUser = userService.getLoginUserFromRequest(request); // 从请求中获取当前登录用户信息
        chatRoomService.ensureTeamMember(teamId, loginUser.getId()); // 验证当前用户是否为指定团队的成员
        Page<ChatMessageResponse> messagePage = chatMessageService.listTeamMessagesWithFallback(teamId, pageNum, pageSize); // 获取团队消息列表，支持降级处理
        return ResultUtils.success(messagePage); // 返回成功响应，包含消息分页数据
    }
}

package com.wobushi041.codemate.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.chat.ChatMessageResponse;
import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ResultUtils;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.ChatMessageService;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;

/**
 * 队伍聊天控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/chat")
@Validated
public class ChatController {

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入聊天室服务依赖
     */
    @Resource
    private ChatRoomService chatRoomService;

    /**
     * 注入聊天消息服务依赖
     */
    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 分页获取指定队伍的历史聊天消息列表接口
     *
     * @param teamId   队伍 id（必须大于等于 1）
     * @param pageNum  分页页码（默认为 1）
     * @param pageSize 每页记录数（默认为 20）
     * @param request  HTTP 请求对象
     * @return 队伍聊天消息分页结果
     */
    @GetMapping("/teams/{teamId}/messages")
    public BaseResponse<Page<ChatMessageResponse>> listTeamMessages(@PathVariable("teamId") @Min(1) Long teamId,
                                                                    @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                                                    @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
                                                                    HttpServletRequest request) {
        // 获取当前登录用户并校验其是否为该队伍成员
        User loginUser = userService.getLoginUserFromRequest(request);
        chatRoomService.ensureTeamMember(teamId, loginUser.getId());

        // 查询队伍聊天历史消息分页数据（含降级兜底消息合并）
        Page<ChatMessageResponse> messagePage = chatMessageService.listTeamMessagesWithFallback(teamId, pageNum, pageSize);
        return ResultUtils.success(messagePage);
    }

}

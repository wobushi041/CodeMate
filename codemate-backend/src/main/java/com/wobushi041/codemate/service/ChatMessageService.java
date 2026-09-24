package com.wobushi041.codemate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.chat.ChatMessageResponse;

/**
 * 聊天消息服务
 *
 * @author wobushi041
 */
public interface ChatMessageService {

    /**
     * 保存队伍聊天消息
     *
     * @param message 聊天消息对象
     */
    void saveMessage(ChatMessageResponse message);

    /**
     * 保存异常场景下的兜底聊天消息
     *
     * @param message 聊天消息对象
     */
    void saveFallbackMessage(ChatMessageResponse message);

    /**
     * 分页获取队伍聊天消息列表（包含兜底消息）
     *
     * @param teamId   队伍 id
     * @param pageNum  请求页码
     * @param pageSize 每页条数
     * @return 队伍聊天消息分页列表
     */
    Page<ChatMessageResponse> listTeamMessagesWithFallback(Long teamId, long pageNum, long pageSize);

}

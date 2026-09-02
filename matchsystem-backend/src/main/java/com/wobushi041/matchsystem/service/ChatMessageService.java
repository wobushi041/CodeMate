package com.wobushi041.matchsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.matchsystem.chat.ChatMessageResponse;

public interface ChatMessageService {

    void saveMessage(ChatMessageResponse message);

    void saveFallbackMessage(ChatMessageResponse message);

    Page<ChatMessageResponse> listTeamMessagesWithFallback(Long teamId, long pageNum, long pageSize);

}

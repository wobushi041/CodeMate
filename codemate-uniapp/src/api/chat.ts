import { request } from '@/request'
import type { ChatMessagePage, PrivateChatSession } from '@/types/chat'

export function startPrivateChat(targetUserId: number): Promise<PrivateChatSession> {
  return request<PrivateChatSession>({
    url: '/chat/private/start',
    method: 'POST',
    data: { targetUserId },
  })
}

export function getPrivateMessages(sessionId: number): Promise<ChatMessagePage> {
  return request<ChatMessagePage>({
    url: '/chat/private/messages',
    data: { sessionId, pageNum: 1, pageSize: 30 },
  })
}

export function getTeamMessages(teamId: number): Promise<ChatMessagePage> {
  return request<ChatMessagePage>({
    url: `/chat/teams/${teamId}/messages`,
    data: { pageNum: 1, pageSize: 30 },
  })
}

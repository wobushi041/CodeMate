import { request } from '@/request'
import type {
  ChatMessagePage,
  PrivateChatSession,
  PrivateChatUnread,
  TeamChatUnread,
} from '@/types/chat'

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

export function getPrivateUnread(): Promise<PrivateChatUnread[]> {
  return request<PrivateChatUnread[]>({
    url: '/chat/private/unread',
    showErrorToast: false,
  })
}

export function markPrivateChatRead(sessionId: number): Promise<boolean> {
  return request<boolean>({
    url: `/chat/private/sessions/${sessionId}/read`,
    method: 'POST',
    showErrorToast: false,
  })
}

export function getTeamUnread(): Promise<TeamChatUnread[]> {
  return request<TeamChatUnread[]>({
    url: '/chat/unread',
    showErrorToast: false,
  })
}

export function markTeamChatRead(teamId: number): Promise<boolean> {
  return request<boolean>({
    url: `/chat/teams/${teamId}/read`,
    method: 'POST',
    showErrorToast: false,
  })
}

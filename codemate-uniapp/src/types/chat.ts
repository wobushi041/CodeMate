import type { UserType } from './api'

export interface ChatMessage {
  type?: string
  teamId?: number
  sessionId?: number
  messageId?: number
  clientMessageId?: string
  fromUserId?: number
  fromUsername?: string
  fromAvatarUrl?: string
  toUserId?: number
  content?: string
  message?: string
  createTime?: string
}

export interface ChatMessagePage {
  records?: ChatMessage[]
  total?: number
  pages?: number
}

export interface PrivateChatSession {
  sessionId: number
  targetUser?: UserType & { userName?: string }
  isTargetOnline?: boolean
}

export interface PrivateChatUnread {
  sessionId: number
  targetUserId: number
  unreadCount: number
}

export interface TeamChatUnread {
  teamId: number
  unreadCount: number
}

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getPrivateUnread,
  getTeamUnread,
  markPrivateChatRead,
  markTeamChatRead,
} from '@/api/chat'
import type { PrivateChatUnread, TeamChatUnread } from '@/types/chat'

export const useUnreadStore = defineStore('unread', () => {
  const privateItems = ref<PrivateChatUnread[]>([])
  const teamItems = ref<TeamChatUnread[]>([])
  let refreshPromise: Promise<void> | null = null

  const privateTotal = computed(() => privateItems.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0))
  const teamTotal = computed(() => teamItems.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0))

  function privateCount(targetUserId: number): number {
    return Number(privateItems.value.find((item) => Number(item.targetUserId) === Number(targetUserId))?.unreadCount || 0)
  }

  function teamCount(teamId: number): number {
    return Number(teamItems.value.find((item) => Number(item.teamId) === Number(teamId))?.unreadCount || 0)
  }

  async function refresh(): Promise<void> {
    if (refreshPromise) return refreshPromise
    const task = Promise.allSettled([getPrivateUnread(), getTeamUnread()])
      .then(([privateResult, teamResult]) => {
        if (privateResult.status === 'fulfilled') privateItems.value = privateResult.value || []
        if (teamResult.status === 'fulfilled') teamItems.value = teamResult.value || []
      })
      .finally(() => {
        refreshPromise = null
      })
    refreshPromise = task
    return task
  }

  async function readPrivate(sessionId: number): Promise<void> {
    privateItems.value = privateItems.value.filter((item) => Number(item.sessionId) !== Number(sessionId))
    try {
      await markPrivateChatRead(sessionId)
    } catch {
      await refresh()
    }
  }

  async function readTeam(teamId: number): Promise<void> {
    teamItems.value = teamItems.value.filter((item) => Number(item.teamId) !== Number(teamId))
    try {
      await markTeamChatRead(teamId)
    } catch {
      await refresh()
    }
  }

  function reset(): void {
    privateItems.value = []
    teamItems.value = []
  }

  return {
    privateTotal,
    teamTotal,
    privateCount,
    teamCount,
    refresh,
    readPrivate,
    readTeam,
    reset,
  }
})

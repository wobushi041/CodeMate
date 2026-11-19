import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser } from '@/api/user'
import type { UniAppLoginData, UserType } from '@/types/api'
import {
  clearLoginStorage,
  getStoredUser,
  getToken,
  onLoginCleared,
  setStoredUser,
  setToken,
} from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<UserType | null>(getStoredUser())
  const isLoggedIn = computed(() => Boolean(token.value))

  function applyLogin(data: UniAppLoginData): void {
    token.value = data.token
    userInfo.value = data.userInfo
    setToken(data.token)
    setStoredUser(data.userInfo)
  }

  function updateUser(user: UserType): void {
    userInfo.value = user
    setStoredUser(user)
  }

  async function fetchCurrentUser(): Promise<UserType> {
    const user = await getCurrentUser()
    updateUser(user)
    return user
  }

  function resetLoginState(): void {
    token.value = ''
    userInfo.value = null
  }

  function clearLogin(): void {
    resetLoginState()
    clearLoginStorage()
  }

  onLoginCleared(resetLoginState)

  return {
    token,
    userInfo,
    isLoggedIn,
    applyLogin,
    updateUser,
    fetchCurrentUser,
    clearLogin,
  }
})

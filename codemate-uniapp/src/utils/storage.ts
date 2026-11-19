import type { UserType } from '@/types/api'

const TOKEN_KEY = 'codemate:uniapp:token'
const USER_INFO_KEY = 'codemate:uniapp:user-info'
const loginClearedListeners = new Set<() => void>()

export function onLoginCleared(listener: () => void): () => void {
  loginClearedListeners.add(listener)
  return () => loginClearedListeners.delete(listener)
}

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getStoredUser(): UserType | null {
  return uni.getStorageSync(USER_INFO_KEY) || null
}

export function setStoredUser(user: UserType): void {
  uni.setStorageSync(USER_INFO_KEY, user)
}

export function clearLoginStorage(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_INFO_KEY)
  loginClearedListeners.forEach((listener) => listener())
}

import { getStoredUser } from '@/utils/storage'

const CONTACTED_KEY = 'codemate:contacted-user-ids'

function getContactedStorageKey(): string {
  const userId = getStoredUser()?.id
  return `${CONTACTED_KEY}:${userId || 'anonymous'}`
}

export function getContactedUserIds(): number[] {
  try {
    const value = uni.getStorageSync(getContactedStorageKey())
    const parsed = typeof value === 'string' ? JSON.parse(value || '[]') : value
    return Array.isArray(parsed) ? parsed.map(Number).filter(Number.isFinite) : []
  } catch {
    return []
  }
}

export function markUserContacted(userId?: number | string | null): void {
  const id = Number(userId)
  if (!Number.isFinite(id) || id <= 0) return
  const ids = getContactedUserIds()
  if (!ids.includes(id)) {
    uni.setStorageSync(getContactedStorageKey(), JSON.stringify([...ids, id]))
  }
}

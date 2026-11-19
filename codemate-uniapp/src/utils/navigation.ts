import { getToken } from '@/utils/storage'

const LOGIN_PAGE = '/pages/auth/login'
const REGISTER_PAGE = '/pages/auth/register'

const TAB_PAGES = new Set([
  '/pages/index/index',
  '/pages/match/index',
  '/pages/team/index',
  '/pages/ai/index',
  '/pages/user/index',
])

function getCurrentFullPath(): string {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  if (!current) return '/pages/index/index'
  return current.$page?.fullPath || `/${current.route || 'pages/index/index'}`
}

function safeDecode(value: string): string {
  try {
    return decodeURIComponent(value)
  } catch {
    return ''
  }
}

export function normalizeRedirect(raw?: string): string {
  const target = safeDecode(raw || '')
  if (!target.startsWith('/') || target.startsWith('//')) return ''
  if (target.startsWith(LOGIN_PAGE) || target.startsWith(REGISTER_PAGE)) return ''
  if (!target.startsWith('/pages/') && !target.startsWith('/subpages/')) return ''
  return target
}

export function redirectToLogin(): void {
  const currentPath = getCurrentFullPath()
  if (currentPath.startsWith(LOGIN_PAGE) || currentPath.startsWith(REGISTER_PAGE)) return

  const redirect = normalizeRedirect(currentPath)
  const query = redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''
  uni.reLaunch({ url: `${LOGIN_PAGE}${query}` })
}

export function redirectAfterLogin(raw?: string): void {
  const target = normalizeRedirect(raw) || '/pages/index/index'
  const path = target.split('?')[0]

  if (TAB_PAGES.has(path)) {
    uni.switchTab({ url: path })
    return
  }

  uni.reLaunch({ url: target })
}

export function requireLogin(): boolean {
  if (getToken()) return true
  redirectToLogin()
  return false
}

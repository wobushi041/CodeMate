import { request } from '@/request'
import type { UserType } from '@/types/api'

export interface UserPage {
  records?: UserType[]
  total?: number
  pages?: number
}

export function getRecommendUsers(pageNum: number, pageSize = 20): Promise<UserPage> {
  return request<UserPage>({ url: '/user/recommend', data: { pageNum, pageSize } })
}

export function getMatchedUsers(num = 20): Promise<UserType[]> {
  return request<UserType[]>({ url: '/user/match', data: { num } })
}

export function refreshMatchedUsers(num = 30): Promise<UserType[]> {
  return request<UserType[]>({ url: '/user/match/withoutRedis', data: { num }, showErrorToast: false })
}

export function searchUsersByTags(tags: string[]): Promise<UserType[]> {
  const query = tags.map((tag) => `tagNameList=${encodeURIComponent(tag)}`).join('&')
  return request<UserType[]>({ url: `/user/search/tags?${query}` })
}

export function updateUser(data: Partial<UserType> & { id: number }): Promise<number> {
  return request<number>({ url: '/user/update', method: 'POST', data: data as Record<string, any> })
}

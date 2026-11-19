import type { UserType } from './api'

export interface TeamType {
  id: number
  name: string
  description: string
  expireTime?: string | null
  maxNum: number
  password?: string
  status: number
  createTime?: string
  updateTime?: string
  userId?: number
  createUser?: UserType
  hasJoinNum?: number
  hasJoin?: boolean
}

export interface TeamFormParams {
  id?: number
  name: string
  description: string
  expireTime?: string | null
  maxNum?: number
  password?: string
  status: number
}

export const TEAM_STATUS_TEXT: Record<number, string> = {
  0: '公开',
  1: '私有',
  2: '加密',
}

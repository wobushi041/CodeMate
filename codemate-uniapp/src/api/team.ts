import { request } from '@/request'
import type { TeamFormParams, TeamType } from '@/types/team'

export function getTeamList(searchText = '', status = 0): Promise<TeamType[]> {
  return request<TeamType[]>({ url: '/team/list', data: { searchText, pageNum: 1, status } })
}

export function getMyCreatedTeams(searchText = ''): Promise<TeamType[]> {
  return request<TeamType[]>({ url: '/team/list/my/create', data: { searchText, pageNum: 1 } })
}

export function getMyJoinedTeams(searchText = ''): Promise<TeamType[]> {
  return request<TeamType[]>({ url: '/team/list/my/join', data: { searchText, pageNum: 1 } })
}

export function getTeamDetail(id: number): Promise<TeamType> {
  return request<TeamType>({ url: '/team/get', data: { id } })
}

export function createTeam(data: TeamFormParams): Promise<number> {
  return request<number>({ url: '/team/add', method: 'POST', data: data as unknown as Record<string, any> })
}

export function updateTeam(data: TeamFormParams): Promise<boolean> {
  return request<boolean>({ url: '/team/update', method: 'POST', data: data as unknown as Record<string, any> })
}

export function joinTeam(teamId: number, password = ''): Promise<boolean> {
  return request<boolean>({ url: '/team/join', method: 'POST', data: { teamId, password } })
}

export function quitTeam(teamId: number): Promise<boolean> {
  return request<boolean>({ url: '/team/quit', method: 'POST', data: { teamId } })
}

export function deleteTeam(id: number): Promise<boolean> {
  return request<boolean>({ url: '/team/delete', method: 'POST', data: id })
}

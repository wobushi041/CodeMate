import { request } from '@/request'
import type {
  LoginParams,
  RegisterParams,
  UniAppLoginData,
  UserType,
} from '@/types/api'

export function loginByUniApp(data: LoginParams): Promise<UniAppLoginData> {
  return request<UniAppLoginData>({
    url: '/user/uniapp/login',
    method: 'POST',
    data,
    showErrorToast: false,
  })
}

export function registerUser(data: RegisterParams): Promise<number> {
  return request<number>({
    url: '/user/register',
    method: 'POST',
    data,
  })
}

export function getCurrentUser(): Promise<UserType> {
  return request<UserType>({
    url: '/user/current',
    method: 'GET',
  })
}

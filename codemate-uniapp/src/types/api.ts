export interface BaseResponse<T> {
  code: number
  data: T
  message?: string
  description?: string
}

export interface UserType {
  id: number
  username?: string
  userAccount?: string
  avatarUrl?: string
  profile?: string
  gender?: number
  phone?: string
  email?: string
  planetCode?: string
  userRole?: number
  userStatus?: number
  createTime?: string
  tags?: string
}

export interface UniAppLoginData {
  token: string
  tokenType: string
  userInfo: UserType
}

export interface LoginParams {
  userAccount: string
  userPassword: string
}

export interface RegisterParams {
  userAccount: string
  userPassword: string
  checkPassword: string
  planetCode: string
}

import { API_BASE_URL } from '@/config/env'
import type { BaseResponse } from '@/types/api'
import { redirectToLogin } from '@/utils/navigation'
import { clearLoginStorage, getToken } from '@/utils/storage'

export type RequestMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  url: string
  method?: RequestMethod
  data?: any
  header?: Record<string, string>
  showErrorToast?: boolean
}

export class RequestError extends Error {
  code?: number
  statusCode?: number

  constructor(message: string, code?: number, statusCode?: number) {
    super(message)
    this.name = 'RequestError'
    this.code = code
    this.statusCode = statusCode
  }
}

function showError(message: string, enabled: boolean): void {
  if (!enabled) return
  uni.showToast({ title: message || '请求失败', icon: 'none', duration: 2500 })
}

export async function request<T>(options: RequestOptions): Promise<T> {
  const showErrorToast = options.showErrorToast !== false
  const token = getToken()
  const url = /^https?:\/\//.test(options.url)
    ? options.url
    : `${API_BASE_URL}${options.url.startsWith('/') ? options.url : `/${options.url}`}`

  try {
    const response = await uni.request({
      url,
      method: options.method || 'GET',
      data: options.data as any,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
    })

    if (response.statusCode < 200 || response.statusCode >= 300) {
      const error = new RequestError(`请求失败（${response.statusCode}）`, undefined, response.statusCode)
      showError(error.message, showErrorToast)
      throw error
    }

    const body = response.data as BaseResponse<T>
    if (!body || typeof body.code !== 'number') {
      const error = new RequestError('服务返回格式异常', undefined, response.statusCode)
      showError(error.message, showErrorToast)
      throw error
    }

    if (body.code === 40100) {
      const message = body.description || body.message || '登录已过期'
      clearLoginStorage()
      showError(message, showErrorToast)
      redirectToLogin()
      throw new RequestError(message, body.code, response.statusCode)
    }

    if (body.code !== 0) {
      const message = body.description || body.message || '请求失败'
      showError(message, showErrorToast)
      throw new RequestError(message, body.code, response.statusCode)
    }

    return body.data
  } catch (error) {
    if (error instanceof RequestError) throw error

    const requestError = new RequestError('网络连接失败，请检查后端服务和网络')
    showError(requestError.message, showErrorToast)
    throw requestError
  }
}

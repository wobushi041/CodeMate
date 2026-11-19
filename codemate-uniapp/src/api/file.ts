import { API_BASE_URL } from '@/config/env'
import type { BaseResponse } from '@/types/api'
import { getToken, clearLoginStorage } from '@/utils/storage'
import { redirectToLogin } from '@/utils/navigation'

export function uploadAvatar(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/file/upload/avatar`,
      filePath,
      name: 'file',
      header: {
        ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
      },
      success: (response) => {
        try {
          if (response.statusCode < 200 || response.statusCode >= 300) {
            throw new Error(`上传失败（${response.statusCode}）`)
          }
          const body = JSON.parse(response.data) as BaseResponse<string>
          if (body.code === 40100) {
            clearLoginStorage()
            redirectToLogin()
            throw new Error(body.description || body.message || '登录已过期')
          }
          if (body.code !== 0 || !body.data) {
            throw new Error(body.description || body.message || '头像上传失败')
          }
          resolve(body.data)
        } catch (error) {
          reject(error)
        }
      },
      fail: () => reject(new Error('头像上传失败，请检查网络')),
    })
  })
}

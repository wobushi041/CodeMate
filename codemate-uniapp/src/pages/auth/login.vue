<template>
  <view class="auth-page">
    <view class="auth-brand">
      <text class="auth-brand__logo">CodeMate</text>
      <text class="auth-brand__slogan">找到和你默契相合的技术拍档</text>
    </view>

    <view class="auth-form">
      <text class="auth-form__title">登录</text>
      <input
        v-model="form.userAccount"
        class="auth-input"
        type="text"
        placeholder="请输入账号"
        placeholder-class="auth-input__placeholder"
        :maxlength="32"
      />
      <input
        v-model="form.userPassword"
        class="auth-input"
        type="password"
        placeholder="请输入密码"
        placeholder-class="auth-input__placeholder"
        :maxlength="64"
        @confirm="submitLogin"
      />
      <button class="auth-submit" :loading="loading" :disabled="loading" @click="submitLogin">
        {{ loading ? '正在登录…' : '登录' }}
      </button>
      <button class="auth-link" @click="goRegister">还没有账号？点击注册</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { loginByUniApp } from '@/api/user'
import { useUserStore } from '@/store/user'
import { redirectAfterLogin } from '@/utils/navigation'
import { getToken } from '@/utils/storage'
import { decodeQueryValue } from '@/utils/query'

const userStore = useUserStore()
const loading = ref(false)
const redirect = ref('')
const form = reactive({
  userAccount: '',
  userPassword: '',
})

onLoad((options) => {
  redirect.value = typeof options?.redirect === 'string' ? options.redirect : ''
  if (typeof options?.account === 'string') {
    form.userAccount = decodeQueryValue(options.account)
  }
  if (getToken()) {
    redirectAfterLogin(redirect.value)
  }
})

async function submitLogin() {
  if (loading.value) return
  if (!form.userAccount.trim() || !form.userPassword) {
    uni.showToast({ title: '请输入账号和密码', icon: 'none' })
    return
  }
  if (form.userAccount.trim().length < 4 || form.userPassword.length < 8) {
    uni.showToast({ title: '账号至少 4 位，密码至少 8 位', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const data = await loginByUniApp({
      userAccount: form.userAccount.trim(),
      userPassword: form.userPassword,
    })
    userStore.applyLogin(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => redirectAfterLogin(redirect.value), 300)
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败'
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goRegister() {
  const query = redirect.value ? `?redirect=${encodeURIComponent(redirect.value)}` : ''
  uni.navigateTo({ url: `/pages/auth/register${query}` })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 120rpx 48rpx 64rpx;
  background: #111827;
}

.auth-brand {
  display: flex;
  margin-bottom: 88rpx;
  flex-direction: column;
  align-items: center;
}

.auth-brand__logo {
  font-size: 56rpx;
  font-weight: 800;
}

.auth-brand__slogan {
  margin-top: 20rpx;
  color: #94a3b8;
  font-size: 27rpx;
}

.auth-form {
  display: flex;
  flex-direction: column;
}

.auth-form__title {
  margin-bottom: 36rpx;
  font-size: 44rpx;
  font-weight: 700;
}

.auth-input {
  width: 100%;
  height: 104rpx;
  margin-bottom: 28rpx;
  padding: 0 32rpx;
  border: 1rpx solid #374151;
  border-radius: 24rpx;
  color: #f8fafc;
  background: #1f2937;
  font-size: 28rpx;
}

.auth-input__placeholder {
  color: #64748b;
}

.auth-submit {
  width: 100%;
  height: 100rpx;
  margin-top: 16rpx;
  border-radius: 999rpx;
  color: #030712;
  background: #f8fafc;
  font-size: 29rpx;
  font-weight: 600;
  line-height: 100rpx;
}

.auth-link {
  margin-top: 28rpx;
  color: #cbd5e1;
  background: transparent;
  font-size: 26rpx;
}
</style>

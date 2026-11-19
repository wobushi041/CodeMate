<template>
  <view class="auth-page">
    <view class="auth-form">
      <text class="auth-form__title">创建账号</text>
      <input v-model="form.userAccount" class="auth-input" placeholder="账号（至少 4 位）" placeholder-class="auth-input__placeholder" />
      <input v-model="form.planetCode" class="auth-input" placeholder="星球编号" placeholder-class="auth-input__placeholder" />
      <input v-model="form.userPassword" class="auth-input" type="password" placeholder="密码（至少 8 位）" placeholder-class="auth-input__placeholder" />
      <input v-model="form.checkPassword" class="auth-input" type="password" placeholder="再次输入密码" placeholder-class="auth-input__placeholder" @confirm="submitRegister" />
      <button class="auth-submit" :loading="loading" :disabled="loading" @click="submitRegister">
        {{ loading ? '正在注册…' : '注册' }}
      </button>
      <button class="auth-link" @click="goLogin">已有账号？返回登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { registerUser } from '@/api/user'

const loading = ref(false)
const redirect = ref('')
const form = reactive({
  userAccount: '',
  planetCode: '',
  userPassword: '',
  checkPassword: '',
})

onLoad((options) => {
  redirect.value = typeof options?.redirect === 'string' ? options.redirect : ''
})

async function submitRegister() {
  if (loading.value) return
  if (form.userAccount.trim().length < 4 || form.userPassword.length < 8 || !form.planetCode.trim()) {
    uni.showToast({ title: '请完整填写账号、编号和密码', icon: 'none' })
    return
  }
  if (form.userPassword !== form.checkPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await registerUser({
      userAccount: form.userAccount.trim(),
      planetCode: form.planetCode.trim(),
      userPassword: form.userPassword,
      checkPassword: form.checkPassword,
    })
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(goLogin, 400)
  } catch {
    // 请求层已展示后端返回的错误信息
  } finally {
    loading.value = false
  }
}

function goLogin() {
  const account = encodeURIComponent(form.userAccount.trim())
  const redirectQuery = redirect.value
    ? `&redirect=${encodeURIComponent(redirect.value)}`
    : ''
  uni.redirectTo({ url: `/pages/auth/login?account=${account}${redirectQuery}` })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 96rpx 48rpx 64rpx;
  background: #111827;
}

.auth-form {
  display: flex;
  flex-direction: column;
}

.auth-form__title {
  margin-bottom: 44rpx;
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

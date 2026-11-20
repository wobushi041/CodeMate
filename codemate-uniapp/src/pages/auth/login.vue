<template>
  <view class="login-page">
    <view class="login-content">
      <view class="login-brand">
        <view class="login-logo-wrap"><image class="login-logo" :src="logo" mode="aspectFit" /></view>
        <text>快速发掘和你默契最合的技术拍档</text>
      </view>
      <view class="login-card">
        <view class="login-field-group">
          <text class="login-label">请输入账号</text>
          <input v-model="form.userAccount" type="text" placeholder="你的账号" :maxlength="32" />
        </view>
        <view class="login-field-group">
          <text class="login-label">请输入密码</text>
          <view class="login-password-wrap">
            <input v-model="form.userPassword" :type="showPassword ? 'text' : 'password'" placeholder="你的密码" :maxlength="64" @confirm="submitLogin" />
            <button @click="showPassword = !showPassword"><wd-icon :name="showPassword ? 'browse-off' : 'browse'" size="20px" /></button>
          </view>
        </view>
        <button class="login-submit" :disabled="loading" @click="submitLogin">
          <wd-icon v-if="loading" name="refresh" class="login-spinner" size="20px" />
          <text>{{ loading ? '正在登录...' : '登录' }}</text>
        </button>
      </view>
      <view class="login-register"><text>还没有账号？</text><button @click="goRegister">点击这里注册</button></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { loginByUniApp } from '@/api/user'
import logo from '@/assets/brand-logo-dark.png'
import { useUserStore } from '@/store/user'
import { redirectAfterLogin } from '@/utils/navigation'
import { getToken } from '@/utils/storage'
import { decodeQueryValue } from '@/utils/query'

const userStore = useUserStore()
const loading = ref(false)
const showPassword = ref(false)
const redirect = ref('')
const form = reactive({ userAccount: '', userPassword: '' })
onLoad((options) => {
  redirect.value = typeof options?.redirect === 'string' ? options.redirect : ''
  if (typeof options?.account === 'string') form.userAccount = decodeQueryValue(options.account)
  if (getToken()) redirectAfterLogin(redirect.value)
})
async function submitLogin() {
  if (loading.value) return
  if (!form.userAccount.trim() || !form.userPassword) { uni.showToast({ title: '请输入账号和密码', icon: 'none' }); return }
  if (form.userAccount.trim().length < 4 || form.userPassword.length < 8) { uni.showToast({ title: '账号至少 4 位，密码至少 8 位', icon: 'none' }); return }
  loading.value = true
  try {
    const data = await loginByUniApp({ userAccount: form.userAccount.trim(), userPassword: form.userPassword })
    userStore.applyLogin(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => redirectAfterLogin(redirect.value), 300)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '登录失败', icon: 'none' })
  } finally { loading.value = false }
}
function goRegister() {
  const query = redirect.value ? `?redirect=${encodeURIComponent(redirect.value)}` : ''
  uni.navigateTo({ url: `/pages/auth/register${query}` })
}
</script>

<style scoped>
.login-page { position: relative; width: 100%; min-height: 100vh; overflow: hidden; color: #242321; background: #faf9f6; }
.login-content { position: relative; z-index: 1; display: flex; width: 100%; min-height: 100vh; padding: calc(52px + var(--status-bar-height)) 24px 100px; flex-direction: column; align-items: center; justify-content: center; }
.login-brand { display: flex; margin-bottom: 64px; flex-direction: column; align-items: center; color: #66635d; font-size: 14px; line-height: 20px; text-align: center; }
.login-logo-wrap { display: grid; width: 100%; margin-bottom: 14px; place-items: center; }
.login-logo { display: block; width: 190px; height: 105px; margin: 0 auto; object-fit: contain; filter: drop-shadow(0 10px 18px rgba(2,6,23,.3)); }
.login-card { width: 88%; max-width: 380px; margin: 0 auto 40px; }
.login-field-group { display: grid; gap: 12px; }
.login-field-group + .login-field-group { margin-top: 28px; }
.login-label { padding-left: 4px; color: #66635d; font-size: 14px; font-weight: 500; line-height: 20px; }
.login-field-group input { width: 100%; height: 56px; padding: 0 24px; border: 1px solid #d9d4ca; border-radius: 16px; color: #242321; background: #ffffff; font-size: 14px; }
.login-password-wrap { position: relative; }
.login-password-wrap input { padding-right: 58px; }
.login-password-wrap button { position: absolute; top: 50%; right: 14px; display: grid; width: 40px; height: 40px; margin: 0; padding: 0; transform: translateY(-50%); place-items: center; border: 0; border-radius: 50%; color: #85817a; background: transparent; }
.login-password-wrap button::after { border: 0; }
.login-submit { display: flex; width: 100%; height: 56px; margin: 28px 0 0; padding: 0; align-items: center; justify-content: center; gap: 10px; border: 0; border-radius: 999px; color: #242321; background: #fff; box-shadow: 0 12px 24px rgba(2,6,23,.28); font-size: 14px; font-weight: 600; }
.login-submit::after { border: 0; }
.login-submit:active { transform: scale(.98); background: #f4f4f5; }
.login-submit:disabled { opacity: .7; }
.login-register { display: flex; align-items: center; justify-content: center; color: #66635d; font-size: 14px; line-height: 20px; }
.login-register button { display: inline; margin: 0; padding: 0; border: 0; color: #66635d; background: transparent; font-size: 14px; font-weight: 600; line-height: 20px; text-decoration: underline; text-underline-offset: 3px; }
.login-register button::after { border: 0; }
.login-spinner { animation: login-spin 1s linear infinite; }
@keyframes login-spin { to { transform: rotate(360deg); } }
@media (max-height:720px) { .login-content { justify-content: flex-start; padding-top: calc(88px + var(--status-bar-height)); padding-bottom: 80px; overflow-y: auto; } .login-brand { margin-bottom: 40px; } .login-logo { width: 160px; height: 89px; } }
.login-page { color: #242321; background: #faf9f6; }
.login-brand p { color: #77736c; }
.login-logo { filter: none; }
.login-field-group label { color: #242321; }
.login-field-group input { border-color: #d9d4ca; color: #242321; background: #fff; }
.login-password-wrap button { color: #77736c; }
.login-password-wrap button:active { color: #242321; background: #f4f1e9; }
.login-submit { color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.login-submit:hover, .login-submit:active { background: #49443f; }
.login-register { color: #77736c; }
.login-register button { color: #977322; }
</style>

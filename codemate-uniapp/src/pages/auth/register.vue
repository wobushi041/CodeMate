<template>
  <view class="register-page">
    <view class="register-content">
      <view class="register-brand">
        <view class="register-logo-wrap"><image class="register-logo" :src="logo" mode="aspectFit" /></view>
        <view class="register-title">注册新账号</view>
      </view>
      <view class="register-card">
        <view class="register-field-group"><text class="register-label">账号</text><input v-model="form.userAccount" placeholder="设置你的账号" :maxlength="32" /></view>
        <view class="register-field-group">
          <text class="register-label">密码(最少8位)</text>
          <view class="register-password-wrap"><input v-model="form.userPassword" :type="showPassword ? 'text' : 'password'" placeholder="设置登录密码" :maxlength="64" /><button @click="showPassword = !showPassword"><wd-icon :name="showPassword ? 'browse-off' : 'browse'" size="20px" /></button></view>
        </view>
        <view class="register-field-group">
          <text class="register-label">确认密码(最少8位)</text>
          <view class="register-password-wrap"><input v-model="form.checkPassword" :type="showCheckPassword ? 'text' : 'password'" placeholder="再次输入登录密码" :maxlength="64" @confirm="submitRegister" /><button @click="showCheckPassword = !showCheckPassword"><wd-icon :name="showCheckPassword ? 'browse-off' : 'browse'" size="20px" /></button></view>
        </view>
        <view class="register-field-group"><text class="register-label">用户编号</text><input v-model="form.planetCode" placeholder="设置你的用户编号" /></view>
        <button class="register-submit" :disabled="loading" @click="submitRegister"><wd-icon v-if="loading" name="refresh" class="register-spinner" size="20px" /><text>{{ loading ? '正在注册...' : '注册' }}</text></button>
      </view>
      <view class="register-login"><text>已有账号？</text><button @click="goLogin">点这里登录</button></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { registerUser } from '@/api/user'
import logo from '@/assets/brand-logo-dark.png'

const loading = ref(false)
const showPassword = ref(false)
const showCheckPassword = ref(false)
const redirect = ref('')
const form = reactive({ userAccount: '', planetCode: '', userPassword: '', checkPassword: '' })
onLoad((options) => { redirect.value = typeof options?.redirect === 'string' ? options.redirect : '' })
async function submitRegister() {
  if (loading.value) return
  if (form.userAccount.trim().length < 4 || form.userPassword.length < 8 || !form.planetCode.trim()) { uni.showToast({ title: '请完整填写账号、编号和密码', icon: 'none' }); return }
  if (form.userPassword !== form.checkPassword) { uni.showToast({ title: '两次输入的密码不一致', icon: 'none' }); return }
  loading.value = true
  try {
    await registerUser({ userAccount: form.userAccount.trim(), planetCode: form.planetCode.trim(), userPassword: form.userPassword, checkPassword: form.checkPassword })
    uni.showToast({ title: '注册成功', icon: 'success' }); setTimeout(goLogin, 400)
  } catch { /* 请求层已展示后端返回的错误信息 */ } finally { loading.value = false }
}
function goLogin() {
  const account = encodeURIComponent(form.userAccount.trim())
  const redirectQuery = redirect.value ? `&redirect=${encodeURIComponent(redirect.value)}` : ''
  uni.redirectTo({ url: `/pages/auth/login?account=${account}${redirectQuery}` })
}
</script>

<style scoped>
.register-page { width: 100%; min-height: 100vh; overflow-x: hidden; color: #242321; background: #faf9f6; }
.register-content { display: flex; width: 100%; min-height: 100vh; padding: calc(36px + var(--status-bar-height)) 24px 44px; flex-direction: column; align-items: center; justify-content: center; }
.register-brand { display: flex; margin-bottom: 28px; flex-direction: column; align-items: center; text-align: center; }
.register-logo-wrap { display: grid; width: 100%; margin-bottom: 14px; place-items: center; }
.register-logo { display: block; width: 190px; height: 105px; margin: 0 auto; object-fit: contain; filter: drop-shadow(0 10px 18px rgba(2,6,23,.3)); }
.register-title { color: #242321; font-size: 28px; font-weight: 800; line-height: 36px; letter-spacing: -.03em; }
.register-card { width: 88%; max-width: 380px; margin: 0 auto 24px; }
.register-field-group { display: grid; gap: 10px; }
.register-field-group + .register-field-group { margin-top: 16px; }
.register-label { padding-left: 4px; color: #66635d; font-size: 14px; font-weight: 500; line-height: 20px; }
.register-field-group input { width: 100%; height: 52px; padding: 0 20px; border: 1px solid #d9d4ca; border-radius: 15px; color: #242321; background: #ffffff; font-size: 14px; }
.register-password-wrap { position: relative; }
.register-password-wrap input { padding-right: 58px; }
.register-password-wrap button { position: absolute; top: 50%; right: 12px; display: grid; width: 40px; height: 40px; margin: 0; padding: 0; transform: translateY(-50%); place-items: center; border: 0; border-radius: 50%; color: #85817a; background: transparent; }
.register-password-wrap button::after { border: 0; }
.register-submit { display: flex; width: 100%; height: 54px; margin: 26px 0 0; padding: 0; align-items: center; justify-content: center; gap: 10px; border: 0; border-radius: 999px; color: #242321; background: #fff; box-shadow: 0 12px 24px rgba(2,6,23,.28); font-size: 14px; font-weight: 600; }
.register-submit::after { border: 0; }
.register-submit:active { transform: scale(.98); background: #f4f4f5; }
.register-submit:disabled { opacity: .7; }
.register-login { display: flex; align-items: center; justify-content: center; color: #66635d; font-size: 14px; line-height: 20px; }
.register-login button { display: inline; margin: 0; padding: 0; border: 0; color: #66635d; background: transparent; font-size: 14px; font-weight: 600; line-height: 20px; text-decoration: underline; text-underline-offset: 3px; }
.register-login button::after { border: 0; }
.register-spinner { animation: register-spin 1s linear infinite; }
@keyframes register-spin { to { transform: rotate(360deg); } }
@media (max-height:760px) { .register-content { justify-content: flex-start; padding-top: calc(28px + var(--status-bar-height)); padding-bottom: 36px; } .register-brand { margin-bottom: 20px; } .register-logo { width: 160px; height: 89px; } .register-title { font-size: 24px; line-height: 30px; } .register-field-group + .register-field-group { margin-top: 14px; } }
.register-page { color: #242321; background: #faf9f6; }
.register-brand p { color: #77736c; }
.register-logo { filter: none; }
.register-field-group label { color: #242321; }
.register-field-group input { border-color: #d9d4ca; color: #242321; background: #fff; }
.register-password-wrap button { color: #77736c; }
.register-password-wrap button:active { color: #242321; background: #f4f1e9; }
.register-submit { color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.register-submit:hover, .register-submit:active { background: #49443f; }
.register-login { color: #77736c; }
.register-login button { color: #977322; }
</style>

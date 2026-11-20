<template>
  <view class="profile-page">
    <view class="profile-header">
      <view class="profile-header__title">个人信息</view>
    </view>

    <view v-if="userStore.userInfo" class="profile-content">
      <view class="profile-summary">
        <view class="profile-avatar">
          <image v-if="avatarUrl && !avatarError" :src="avatarUrl" mode="aspectFill" @error="avatarError = true" />
          <view v-else class="profile-avatar__placeholder">
            <wd-icon name="image" size="48px" />
            <text>暂时无法查看</text>
          </view>
          <view class="profile-avatar__shade" />
        </view>
        <view class="profile-name">{{ displayName }}</view>
        <view class="profile-tags">
          <text class="profile-tags__label">我的标签:</text>
          <text v-for="tag in userTags" :key="tag" class="profile-tag">{{ tag }}</text>
          <text v-if="!userTags.length" class="profile-tag profile-tag--empty">未设置</text>
        </view>
      </view>

      <view class="profile-menu">
        <button class="profile-menu__item" @click="go('/subpages/user/edit')">
          <text>编辑个人信息</text>
          <wd-icon name="arrow-right" size="20px" />
        </button>
        <button class="profile-menu__item" @click="go('/subpages/user/created')">
          <text>我创建的队伍</text>
          <wd-icon name="arrow-right" size="20px" />
        </button>
        <button class="profile-menu__item" @click="go('/subpages/user/joined')">
          <text>我加入的队伍</text>
          <wd-icon name="arrow-right" size="20px" />
        </button>
      </view>

      <view class="profile-logout">
        <button class="logout-button" @click="logout">
          <wd-icon name="logout" size="20px" />
          <text>退出登录</text>
        </button>
      </view>
    </view>
    <AppTabbar active="user" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabbar from '@/components/AppTabbar.vue'
import { useUserStore } from '@/store/user'
import { normalizeTags } from '@/utils/format'
import { redirectToLogin, requireLogin } from '@/utils/navigation'

const userStore = useUserStore()
const refreshing = ref(false)
const avatarError = ref(false)
const displayName = computed(() => userStore.userInfo?.username || userStore.userInfo?.userAccount || 'CodeMate 用户')
const avatarUrl = computed(() => userStore.userInfo?.avatarUrl || '')
const userTags = computed(() => normalizeTags(userStore.userInfo?.tags))

onShow(() => { uni.hideTabBar({ animation: false }); if (requireLogin()) refreshUser() })
async function refreshUser() {
  if (refreshing.value) return
  refreshing.value = true
  try { await userStore.fetchCurrentUser() } catch { /* 请求层统一提示 */ } finally { refreshing.value = false }
}
function go(url: string) { uni.navigateTo({ url }) }
async function logout() {
  const result = await uni.showModal({ title: '确认退出', content: '是否确认退出登录？', confirmColor: '#ad4a46' })
  if (!result.confirm) return
  userStore.clearLogin()
  redirectToLogin()
}
</script>

<style scoped>
.profile-page, .profile-header, .profile-content, .profile-avatar, .profile-menu__item, .logout-button { box-sizing: border-box; }
.profile-page { width: 100%; min-height: 100vh; color: #242321; background: #faf9f6; }
.profile-header { display: flex; min-height: 54px; padding: calc(22px + var(--status-bar-height)) 16px 12px; align-items: center; justify-content: center; background: #faf9f6; }
.profile-header__title { color: #242321; font-size: 20px; font-weight: 700; line-height: 28px; letter-spacing: -.025em; text-align: center; }
.profile-content { width: 100%; padding: 28px 16px calc(48px + 64px + env(safe-area-inset-bottom)); overflow-x: hidden; }
.profile-summary { display: flex; margin-bottom: 48px; flex-direction: column; align-items: center; }
.profile-avatar { position: relative; width: 128px; height: 128px; margin-bottom: 24px; overflow: hidden; border: 4px solid rgba(255,255,255,.1); border-radius: 50%; background: #ffffff; box-shadow: 0 18px 35px rgba(2,6,23,.45); }
.profile-avatar image { width: 100%; height: 100%; }
.profile-avatar__shade { position: absolute; background: linear-gradient(to bottom,transparent 55%,rgba(0,0,0,.2)); pointer-events: none; inset: 0; }
.profile-avatar__placeholder { display: flex; width: 100%; height: 100%; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #85817a; }
.profile-avatar__placeholder text { color: #85817a; font-size: 12px; font-weight: 500; }
.profile-name { max-width: 100%; margin-bottom: 12px; overflow: hidden; color: #242321; font-size: 24px; font-weight: 800; line-height: 32px; letter-spacing: -.025em; text-align: center; text-overflow: ellipsis; white-space: nowrap; }
.profile-tags { display: flex; max-width: 100%; flex-wrap: wrap; align-items: center; justify-content: center; gap: 10px; color: #66635d; font-size: 14px; }
.profile-tags__label { color: #85817a; font-weight: 500; }
.profile-tag { padding: 6px 16px; border-radius: 999px; color: #bd9226; background: #eae7e0; font-size: 12px; font-weight: 600; line-height: 16px; }
.profile-tag--empty { color: #85817a; }
.profile-menu { display: flex; width: 100%; margin-bottom: 64px; flex-direction: column; gap: 16px; }
.profile-menu__item { display: flex; width: 100%; min-height: 64px; margin: 0; padding: 18px 20px; align-items: center; justify-content: space-between; border: 1px solid rgba(51,65,85,.5); border-radius: 16px; color: #242321; background: #ffffff; box-shadow: 0 10px 24px rgba(2,6,23,.24); font-size: 16px; font-weight: 600; line-height: 24px; text-align: left; }
.profile-menu__item::after, .logout-button::after { border: 0; }
.profile-menu__item wd-icon { flex: 0 0 auto; color: #85817a; }
.profile-menu__item:active { transform: scale(.985); background: rgba(51,65,85,.72); }
.profile-menu__item:active wd-icon { color: #fff; transform: translateX(2px); }
.profile-logout { padding: 0 16px; }
.logout-button { display: flex; width: 100%; height: 56px; margin: 0; padding: 0 20px; align-items: center; justify-content: center; gap: 12px; border: 1px solid #ead1ca; border-radius: 999px; color: #a34d47; background: rgba(69,10,10,.6); box-shadow: 0 16px 32px rgba(2,6,23,.34); font-size: 15px; font-weight: 600; }
.logout-button:active { transform: scale(.98); background: rgba(127,29,29,.72); }
@media (max-width:359px) { .profile-content { padding-right: 12px; padding-left: 12px; } .profile-logout { padding-right: 4px; padding-left: 4px; } }
.profile-header { color: #242321; background: rgba(250,249,246,.92); }
.profile-header__title { color: #242321; font-size: 21px; }
.profile-content { display: flex; min-height: calc(100vh - 54px - var(--status-bar-height)); padding: 26px 18px calc(100px + env(safe-area-inset-bottom)); flex-direction: column; }
.profile-summary { margin-bottom: 35px; }
.profile-avatar { border-color: #fff; background: #eeeae2; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.profile-avatar__placeholder, .profile-avatar__placeholder text { color: #77736c; }
.profile-name { color: #242321; font-size: 28px; }
.profile-tags, .profile-tags__label { color: #77736c; }
.profile-tag { color: #8c6c24; background: #f8f0dd; }
.profile-tag--empty { color: #77736c; }
.profile-menu { gap: 12px; margin-bottom: 0; }
.profile-menu__item { border-color: #eae7e0; border-radius: 18px; color: #242321; background: #fff; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.profile-menu__item wd-icon { color: #bd9226; }
.profile-menu__item:active { background: #f4f1e9; }
.profile-logout { margin-top: auto; padding-top: 36px; }
.logout-button { border-color: #ead1ca; color: #a34d47; background: #fff6f3; box-shadow: none; }
.logout-button:active { background: #f9eae5; }
</style>

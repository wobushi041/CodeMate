<template>
  <view class="profile-page">
    <view class="profile-summary">
      <image v-if="userStore.userInfo?.avatarUrl" class="profile-avatar" :src="userStore.userInfo.avatarUrl" mode="aspectFill" />
      <view v-else class="profile-avatar profile-avatar--fallback">{{ displayName.slice(0, 1) }}</view>
      <text class="profile-name">{{ displayName }}</text>
      <view class="profile-tags">
        <text v-for="tag in userTags" :key="tag" class="profile-tag">{{ tag }}</text>
        <text v-if="!userTags.length" class="profile-tag">未设置标签</text>
      </view>
    </view>

    <view class="profile-menu">
      <button @click="go('/subpages/user/edit')">编辑个人信息<text>›</text></button>
      <button @click="go('/subpages/user/created')">我创建的队伍<text>›</text></button>
      <button @click="go('/subpages/user/joined')">我加入的队伍<text>›</text></button>
      <button @click="refreshUser">刷新个人信息<text>↻</text></button>
    </view>

    <button class="profile-logout" @click="logout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { normalizeTags } from '@/utils/format'
import { redirectToLogin, requireLogin } from '@/utils/navigation'

const userStore = useUserStore()
const displayName = computed(() => userStore.userInfo?.username || userStore.userInfo?.userAccount || 'CodeMate 用户')
const userTags = computed(() => normalizeTags(userStore.userInfo?.tags))

onShow(() => {
  if (requireLogin()) refreshUser()
})

async function refreshUser() {
  try {
    await userStore.fetchCurrentUser()
  } catch {
    // 请求层统一提示
  }
}

function go(url: string) {
  uni.navigateTo({ url })
}

function logout() {
  userStore.clearLogin()
  redirectToLogin()
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 56rpx 32rpx;
  background: #0f172a;
}

.profile-summary {
  display: flex;
  margin-bottom: 48rpx;
  flex-direction: column;
  align-items: center;
}

.profile-avatar {
  display: flex;
  width: 176rpx;
  height: 176rpx;
  align-items: center;
  justify-content: center;
  border: 6rpx solid #334155;
  border-radius: 50%;
  background: #1e293b;
}

.profile-avatar--fallback {
  color: #f8fafc;
  font-size: 64rpx;
  font-weight: 700;
}

.profile-name {
  max-width: 90%;
  margin-top: 28rpx;
  overflow: hidden;
  font-size: 40rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-tags {
  display: flex;
  margin-top: 20rpx;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12rpx;
}

.profile-tag {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  color: #cbd5e1;
  background: #1e293b;
  font-size: 22rpx;
}

.profile-menu {
  overflow: hidden;
  border: 1rpx solid #334155;
  border-radius: 28rpx;
  background: #1e293b;
}

.profile-menu button {
  display: flex;
  height: 100rpx;
  padding: 0 28rpx;
  align-items: center;
  justify-content: space-between;
  border-radius: 0;
  color: #f8fafc;
  background: transparent;
  font-size: 28rpx;
}

.profile-menu button + button {
  border-top: 1rpx solid #334155;
}

.profile-menu text {
  color: #64748b;
  font-size: 36rpx;
}

.profile-logout {
  margin-top: 40rpx;
  color: #fecaca;
  background: #1e293b;
  font-size: 28rpx;
}
</style>

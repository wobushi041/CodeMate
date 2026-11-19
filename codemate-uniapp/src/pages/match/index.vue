<template>
  <view class="match-page">
    <view class="match-header">
      <view>
        <text class="match-header__title">伙伴匹配</text>
        <text class="match-header__subtitle">基于技术标签寻找更合适的伙伴</text>
      </view>
      <button @click="loadUsers">重新匹配</button>
    </view>

    <view v-if="loading" class="match-state">正在计算匹配度…</view>
    <view v-else-if="!currentPartner" class="match-state">
      <text>暂时没有匹配到伙伴</text>
      <button class="match-primary" @click="loadUsers">再试一次</button>
    </view>

    <template v-else>
      <view class="match-progress">{{ currentIndex + 1 }} / {{ users.length }}</view>
      <PartnerCard
        :user="currentPartner"
        :contacted="contactedIds.includes(Number(currentPartner.id))"
        :contacting="contacting"
        @contact="contactUser"
      />
      <view class="match-actions">
        <button @click="previousPartner">上一位</button>
        <button class="match-primary" @click="nextPartner">下一位</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PartnerCard from '@/components/PartnerCard.vue'
import { startPrivateChat } from '@/api/chat'
import { getMatchedUsers } from '@/api/partner'
import type { UserType } from '@/types/api'
import { getContactedUserIds, markUserContacted } from '@/utils/contacted'
import { requireLogin } from '@/utils/navigation'

const users = ref<UserType[]>([])
const currentIndex = ref(0)
const loading = ref(false)
const loaded = ref(false)
const contacting = ref(false)
const contactedIds = ref<number[]>(getContactedUserIds())
const currentPartner = computed(() => users.value[currentIndex.value] || null)

onShow(() => {
  if (!requireLogin()) return
  contactedIds.value = getContactedUserIds()
  if (!loaded.value) loadUsers()
})

async function loadUsers() {
  if (loading.value) return
  loading.value = true
  try {
    users.value = await getMatchedUsers(20)
    currentIndex.value = 0
    loaded.value = true
  } catch {
    // 请求层统一提示
  } finally {
    loading.value = false
  }
}

function nextPartner() {
  currentIndex.value = users.value.length ? (currentIndex.value + 1) % users.value.length : 0
}

function previousPartner() {
  currentIndex.value = users.value.length
    ? (currentIndex.value - 1 + users.value.length) % users.value.length
    : 0
}

async function contactUser(user: UserType) {
  if (contacting.value) return
  contacting.value = true
  try {
    const session = await startPrivateChat(Number(user.id))
    markUserContacted(user.id)
    contactedIds.value = getContactedUserIds()
    const name = session.targetUser?.userName || session.targetUser?.username || user.username || '编程伙伴'
    const avatar = session.targetUser?.avatarUrl || user.avatarUrl || ''
    uni.navigateTo({
      url: `/subpages/chat/private?sessionId=${session.sessionId}&targetUserId=${user.id}&targetUsername=${encodeURIComponent(name)}&targetAvatarUrl=${encodeURIComponent(avatar)}&isOnline=${session.isTargetOnline ? 1 : 0}`,
    })
  } catch {
    // 请求层统一提示
  } finally {
    contacting.value = false
  }
}
</script>

<style scoped>
.match-page {
  min-height: 100vh;
  padding: 40rpx 28rpx;
  background: #0f172a;
}

.match-header {
  display: flex;
  margin-bottom: 40rpx;
  align-items: center;
  justify-content: space-between;
}

.match-header__title,
.match-header__subtitle {
  display: block;
}

.match-header__title {
  font-size: 42rpx;
  font-weight: 800;
}

.match-header__subtitle {
  margin-top: 10rpx;
  color: #94a3b8;
  font-size: 24rpx;
}

.match-header button,
.match-actions button {
  color: #cbd5e1;
  background: #1e293b;
  font-size: 24rpx;
}

.match-state {
  display: flex;
  min-height: 55vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28rpx;
  color: #94a3b8;
  font-size: 27rpx;
}

.match-progress {
  margin-bottom: 20rpx;
  color: #64748b;
  font-size: 23rpx;
  text-align: center;
}

.match-actions {
  display: flex;
  gap: 20rpx;
}

.match-actions button {
  flex: 1;
}

.match-primary {
  color: #0f172a !important;
  background: #f8fafc !important;
}
</style>

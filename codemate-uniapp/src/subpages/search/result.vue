<template>
  <view class="search-result">
    <view v-if="loading" class="search-result__state">正在搜索伙伴…</view>
    <view v-else-if="!users.length" class="search-result__state">搜索结果为空</view>
    <template v-else>
      <PartnerCard
        v-for="user in users"
        :key="user.id"
        :user="user"
        :contacting="contactingUserId === Number(user.id)"
        @contact="contactUser"
      />
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import PartnerCard from '@/components/PartnerCard.vue'
import { startPrivateChat } from '@/api/chat'
import { getRecommendUsers, searchUsersByTags } from '@/api/partner'
import type { UserType } from '@/types/api'
import { markUserContacted } from '@/utils/contacted'
import { requireLogin } from '@/utils/navigation'
import { decodeQueryJson } from '@/utils/query'

const users = ref<UserType[]>([])
const loading = ref(false)
const contactingUserId = ref<number | null>(null)
const tags = ref<string[]>([])

onLoad((options) => {
  if (!requireLogin()) return
  const parsed = decodeQueryJson<unknown>(options?.tags, [])
  tags.value = Array.isArray(parsed) ? parsed.map(String) : []
  loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    if (tags.value.length) {
      users.value = await searchUsersByTags(tags.value)
    } else {
      const page = await getRecommendUsers(1, 100)
      users.value = page.records || []
    }
  } catch {
    // 请求层统一提示
  } finally {
    loading.value = false
  }
}

async function contactUser(user: UserType) {
  if (contactingUserId.value !== null) return
  contactingUserId.value = Number(user.id)
  try {
    const session = await startPrivateChat(Number(user.id))
    markUserContacted(user.id)
    const name = session.targetUser?.userName || session.targetUser?.username || user.username || '编程伙伴'
    const avatar = session.targetUser?.avatarUrl || user.avatarUrl || ''
    uni.navigateTo({
      url: `/subpages/chat/private?sessionId=${session.sessionId}&targetUserId=${user.id}&targetUsername=${encodeURIComponent(name)}&targetAvatarUrl=${encodeURIComponent(avatar)}&isOnline=${session.isTargetOnline ? 1 : 0}`,
    })
  } catch {
    // 请求层统一提示
  } finally {
    contactingUserId.value = null
  }
}
</script>

<style scoped>
.search-result {
  min-height: 100vh;
  padding: 28rpx;
}

.search-result__state {
  padding: 100rpx 24rpx;
  color: #64748b;
  text-align: center;
}
</style>

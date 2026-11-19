<template>
  <view class="home-page">
    <view class="home-header">
      <view>
        <text class="home-header__title">CodeMate</text>
        <text class="home-header__subtitle">快速发掘和你默契相合的技术拍档</text>
      </view>
      <button class="home-header__notice" @click="showNotice">通知</button>
    </view>

    <view class="home-search" @click="goSearch">按标签搜索技术伙伴…</view>

    <scroll-view class="home-filters" scroll-x enhanced :show-scrollbar="false">
      <view class="home-filters__inner">
        <button
          v-for="filter in filters"
          :key="filter.name"
          class="home-filter"
          :class="{ 'home-filter--active': activeFilter === filter.name }"
          @click="activeFilter = filter.name"
        >{{ filter.name }}</button>
      </view>
    </scroll-view>

    <view class="home-section-title">
      <text>推荐列表（{{ filteredUsers.length }}人）</text>
      <button @click="sortDescending = !sortDescending">切换排序</button>
    </view>

    <view v-if="loading && !users.length" class="home-state">正在加载推荐伙伴…</view>
    <view v-else-if="!filteredUsers.length" class="home-state">没有找到符合条件的伙伴</view>

    <PartnerCard
      v-for="user in filteredUsers"
      :key="user.id"
      :user="user"
      :contacted="contactedIds.includes(Number(user.id))"
      :contacting="contactingUserId === Number(user.id)"
      @contact="contactUser"
    />

    <view v-if="users.length" class="home-load-more" @click="loadNextPage">
      {{ loadingMore ? '正在加载…' : finished ? '没有更多推荐伙伴了' : '点击加载更多' }}
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import PartnerCard from '@/components/PartnerCard.vue'
import { startPrivateChat } from '@/api/chat'
import { getRecommendUsers } from '@/api/partner'
import type { UserType } from '@/types/api'
import { getContactedUserIds, markUserContacted } from '@/utils/contacted'
import { normalizeTags } from '@/utils/format'
import { requireLogin } from '@/utils/navigation'

const filters = [
  { name: '全部', keywords: [] },
  { name: '前端开发', keywords: ['vue', 'react', 'javascript', 'typescript', 'html', 'css', 'tailwind', 'nextjs'] },
  { name: '后端技术', keywords: ['java', 'spring', 'mysql', 'go', 'python', 'node', 'redis'] },
  { name: 'AI/深度学习', keywords: ['ai', 'rag', 'pytorch', 'llm', 'deepseek', '机器学习', '深度学习'] },
  { name: '移动端', keywords: ['android', 'ios', 'flutter', 'swift', 'kotlin', 'react native'] },
]

const activeFilter = ref('全部')
const sortDescending = ref(true)
const users = ref<UserType[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const pageNum = ref(1)
const pageSize = 20
const loaded = ref(false)
const contactingUserId = ref<number | null>(null)
const contactedIds = ref<number[]>(getContactedUserIds())

const filteredUsers = computed(() => {
  const filter = filters.find((item) => item.name === activeFilter.name) || filters[0]
  const result = users.value.filter((user) => {
    if (!filter.keywords.length) return true
    const tags = normalizeTags(user.tags).map((tag) => tag.toLowerCase())
    return filter.keywords.some((keyword) => tags.some((tag) => tag.includes(keyword)))
  })
  return sortDescending.value ? result : [...result].reverse()
})

onShow(() => {
  if (!requireLogin()) return
  contactedIds.value = getContactedUserIds()
  if (!loaded.value) loadPage(1)
})

onReachBottom(() => loadNextPage())

async function loadPage(targetPage: number) {
  if (loading.value || loadingMore.value || (targetPage > 1 && finished.value)) return
  if (targetPage === 1) loading.value = true
  else loadingMore.value = true

  try {
    const page = await getRecommendUsers(targetPage, pageSize)
    const records = page.records || []
    if (targetPage === 1) {
      users.value = records
    } else {
      const ids = new Set(users.value.map((user) => user.id))
      users.value = [...users.value, ...records.filter((user) => !ids.has(user.id))]
    }
    pageNum.value = targetPage
    finished.value = records.length < pageSize
      || (Number(page.pages) > 0 && targetPage >= Number(page.pages))
      || (Number(page.total) > 0 && users.value.length >= Number(page.total))
    loaded.value = true
  } catch {
    // 请求层统一提示
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadNextPage() {
  if (!loading.value && !loadingMore.value && !finished.value) loadPage(pageNum.value + 1)
}

function showNotice() {
  uni.showToast({ title: '暂无新通知', icon: 'none' })
}

function goSearch() {
  uni.navigateTo({ url: '/subpages/search/index' })
}

async function contactUser(user: UserType) {
  if (contactingUserId.value !== null) return
  contactingUserId.value = Number(user.id)
  try {
    const session = await startPrivateChat(Number(user.id))
    markUserContacted(user.id)
    contactedIds.value = getContactedUserIds()
    const targetName = session.targetUser?.userName || session.targetUser?.username || user.username || '编程伙伴'
    const targetAvatar = session.targetUser?.avatarUrl || user.avatarUrl || ''
    uni.navigateTo({
      url: `/subpages/chat/private?sessionId=${session.sessionId}&targetUserId=${user.id}&targetUsername=${encodeURIComponent(targetName)}&targetAvatarUrl=${encodeURIComponent(targetAvatar)}&isOnline=${session.isTargetOnline ? 1 : 0}`,
    })
  } catch {
    // 请求层统一提示
  } finally {
    contactingUserId.value = null
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 36rpx 28rpx 48rpx;
  background: #0f172a;
}

.home-header {
  display: flex;
  margin-bottom: 32rpx;
  align-items: center;
  justify-content: space-between;
}

.home-header__title,
.home-header__subtitle {
  display: block;
}

.home-header__title {
  font-size: 44rpx;
  font-weight: 800;
}

.home-header__subtitle {
  margin-top: 8rpx;
  color: #94a3b8;
  font-size: 24rpx;
}

.home-header__notice,
.home-section-title button {
  padding: 0 20rpx;
  color: #cbd5e1;
  background: #1e293b;
  font-size: 23rpx;
}

.home-search {
  height: 88rpx;
  margin-bottom: 24rpx;
  padding: 0 28rpx;
  border: 1rpx solid #334155;
  border-radius: 24rpx;
  color: #64748b;
  background: #1e293b;
  font-size: 26rpx;
  line-height: 88rpx;
}

.home-filters {
  width: 100%;
  margin-bottom: 28rpx;
  white-space: nowrap;
}

.home-filters__inner {
  display: flex;
  gap: 16rpx;
}

.home-filter {
  flex: 0 0 auto;
  margin: 0;
  padding: 14rpx 26rpx;
  border-radius: 999rpx;
  color: #cbd5e1;
  background: #1e293b;
  font-size: 24rpx;
}

.home-filter--active {
  color: #0f172a;
  background: #f8fafc;
}

.home-section-title {
  display: flex;
  margin-bottom: 24rpx;
  align-items: center;
  justify-content: space-between;
  font-size: 29rpx;
  font-weight: 700;
}

.home-state,
.home-load-more {
  padding: 56rpx 24rpx;
  color: #64748b;
  font-size: 25rpx;
  text-align: center;
}

.home-load-more {
  padding: 24rpx;
}
</style>

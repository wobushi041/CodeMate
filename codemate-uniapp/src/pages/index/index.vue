<template>
  <view class="home-page">
    <view class="home-header">
      <view class="home-header__copy">
        <view class="home-header__title">CodeMate</view>
        <view class="home-header__subtitle">快速发掘和你默契最合的技术拍档</view>
      </view>
      <button class="notice-button" @click="showNotice">
        <wd-icon name="notification" size="20px" />
        <view class="notice-badge" />
      </button>
    </view>

    <AppSearchBox trigger placeholder="按标签搜索技术伙伴..." @activate="goSearch" />

    <scroll-view class="filter-list" scroll-x enhanced :show-scrollbar="false">
      <view class="filter-list__inner">
        <button
          v-for="filter in filters"
          :key="filter.name"
          class="filter-button"
          :class="{ 'filter-button--active': activeFilter === filter.name }"
          @click="activeFilter = filter.name"
        >{{ filter.name }}</button>
      </view>
    </scroll-view>

    <view class="partner-section-heading">
      <view class="partner-section-title">推荐列表 <text>({{ filteredUsers.length }}人)</text></view>
      <button @click="sortDescending = !sortDescending">按活跃度排序<wd-icon name="filter1" size="16px" /></button>
    </view>

    <PartnerCardList v-if="loading && !users.length">
      <PartnerCard v-for="item in 4" :key="item" loading />
    </PartnerCardList>

    <template v-else-if="filteredUsers.length">
      <PartnerCardList>
        <PartnerCard
          v-for="user in filteredUsers"
          :key="user.id"
          :user="user"
          :contacted="contactedIds.includes(Number(user.id))"
          :contacting="contactingUserId === Number(user.id)"
          @contact="contactUser"
        />
      </PartnerCardList>
      <view class="scroll-load-status" :class="{ 'scroll-load-status--finished': finished }" @click="loadNextPage">
        <wd-icon v-if="loadingMore" name="refresh" class="is-spinning" size="16px" />
        <text>{{ loadingMore ? '正在加载下 20 位伙伴...' : finished ? '— 没有更多推荐伙伴了 —' : '上拉或触底加载下 20 位伙伴' }}</text>
      </view>
    </template>

    <view v-else class="partner-empty">
      <wd-icon name="usergroup" size="58px" />
      <view>没有找到符合条件的伙伴</view>
    </view>
    <AppTabbar active="home" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import AppSearchBox from '@/components/AppSearchBox.vue'
import AppTabbar from '@/components/AppTabbar.vue'
import PartnerCard from '@/components/PartnerCard.vue'
import PartnerCardList from '@/components/PartnerCardList.vue'
import { startPrivateChat } from '@/api/chat'
import { getRecommendUsers } from '@/api/partner'
import type { UserType } from '@/types/api'
import { getContactedUserIds, markUserContacted } from '@/utils/contacted'
import { normalizeTags } from '@/utils/format'
import { requireLogin } from '@/utils/navigation'
import { useUnreadStore } from '@/store/unread'

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
const unreadStore = useUnreadStore()

const filteredUsers = computed(() => {
  const filter = filters.find((item) => item.name === activeFilter.value) || filters[0]
  const result = users.value.filter((user) => {
    if (!filter.keywords.length) return true
    const tags = normalizeTags(user.tags).map((tag) => tag.toLowerCase())
    return filter.keywords.some((keyword) => tags.some((tag) => tag.includes(keyword)))
  })
  return sortDescending.value ? result : [...result].reverse()
})

onShow(() => {
  uni.hideTabBar({ animation: false })
  if (!requireLogin()) return
  void unreadStore.refresh()
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
    if (targetPage === 1) users.value = records
    else {
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
function showNotice() { uni.showToast({ title: '暂无新通知', icon: 'none' }) }
function goSearch() { uni.navigateTo({ url: '/subpages/search/index' }) }

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
.home-page { width: 100%; max-width: 100%; min-height: 100vh; padding: calc(30px + var(--status-bar-height)) 16px calc(88px + env(safe-area-inset-bottom)); overflow-x: hidden; color: #242321; background: #faf9f6; }
.home-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.home-header__copy { min-width: 0; }
.home-header__title { color: #242321; font-size: 24px; font-weight: 700; line-height: 32px; letter-spacing: -.025em; }
.home-header__subtitle { margin-top: 4px; color: #66635d; font-size: 14px; line-height: 20px; }
.notice-button { position: relative; display: grid; width: 40px; height: 40px; margin: 0; padding: 0; flex: 0 0 auto; place-items: center; border: 0; border-radius: 50%; color: #85817a; background: #ffffff; }
.notice-button::after { border: 0; }
.notice-button:active { transform: scale(.95); }
.notice-badge { position: absolute; top: 0; right: 0; width: 10px; height: 10px; border: 2px solid #faf9f6; border-radius: 50%; background: #ad4a46; }
.filter-list { width: 100%; margin-bottom: 24px; padding-bottom: 4px; white-space: nowrap; }
.filter-list__inner { display: inline-flex; align-items: center; gap: 10px; }
.filter-button { flex: 0 0 auto; margin: 0; padding: 10px 20px; border: 0; border-radius: 999px; color: #66635d; background: #ffffff; font-size: 14px; line-height: 20px; white-space: nowrap; }
.filter-button::after { border: 0; }
.filter-button:active { transform: scale(.95); }
.filter-button--active { color: #242321; background: #fff; font-weight: 600; }
.partner-section-heading { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 20px; }
.partner-section-title { color: #242321; font-size: 18px; font-weight: 600; line-height: 28px; }
.partner-section-title text { color: #85817a; font-size: 14px; font-weight: 400; }
.partner-section-heading button { display: flex; margin: 0; padding: 0; align-items: center; gap: 6px; border: 0; color: #242321; background: transparent; font-size: 14px; font-weight: 500; line-height: 20px; white-space: nowrap; }
.partner-section-heading button::after { border: 0; }
.partner-section-heading wd-icon { color: #85817a; }
.partner-empty { padding: 64px 20px; color: #85817a; font-size: 14px; text-align: center; }
.partner-empty wd-icon { display: block; margin: 0 auto 16px; opacity: .2; }
.scroll-load-status { display: flex; width: 100%; padding: 20px 16px 28px; align-items: center; justify-content: center; gap: 8px; color: #85817a; font-size: 13px; line-height: 20px; text-align: center; }
.scroll-load-status--finished { color: #85817a; font-size: 12px; }
.is-spinning { color: #bd9226; animation: spin .8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.home-header__title { color: #242321; font-size: 32px; line-height: 38px; }
.home-header__subtitle { color: #77736c; font-size: 13px; }
.notice-button { border: 1px solid #eae7e0; color: #242321; background: #fff; box-shadow: 0 5px 16px rgba(39,34,27,.05); }
.notice-badge { border-color: #fff; }
.filter-list__inner { gap: 8px; }
.filter-button { border: 1px solid #eae7e0; color: #77736c; background: #fff; font-size: 13px; }
.filter-button--active { border-color: #242321; color: #fff; background: #242321; }
.partner-section-title { color: #242321; font-size: 20px; }
.partner-section-title text { color: #77736c; font-size: 12px; }
.partner-section-heading button { color: #77736c; font-size: 12px; }
.partner-section-heading wd-icon { color: #bd9226; }
.scroll-load-status, .scroll-load-status--finished, .partner-empty { color: #77736c; }
</style>

<template>
  <view class="team-page">
    <view class="team-header">
      <text class="team-header__title">找队伍</text>
      <text class="team-header__subtitle">发现并加入适合你的技术队伍</text>
      <input
        v-model="searchText"
        class="team-search"
        type="text"
        placeholder="搜索队伍"
        confirm-type="search"
        @confirm="loadTeams"
      />
      <view class="team-tabs">
        <button :class="{ 'team-tabs--active': status === 0 }" @click="changeStatus(0)">公开</button>
        <button :class="{ 'team-tabs--active': status === 2 }" @click="changeStatus(2)">加密</button>
      </view>
    </view>

    <view v-if="loading" class="team-state">正在加载队伍…</view>
    <view v-else-if="!teams.length" class="team-state">没有找到符合条件的队伍</view>
    <TeamCardList
      v-else
      :team-list="teams"
      :current-user-id="userStore.userInfo?.id"
      @refresh="loadTeams"
    />

    <button class="team-create" @click="goCreate">＋</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TeamCardList from '@/components/TeamCardList.vue'
import { getTeamList } from '@/api/team'
import { useUserStore } from '@/store/user'
import type { TeamType } from '@/types/team'
import { requireLogin } from '@/utils/navigation'

const userStore = useUserStore()
const teams = ref<TeamType[]>([])
const searchText = ref('')
const status = ref(0)
const loading = ref(false)
let loadSerial = 0

onShow(() => {
  if (requireLogin()) loadTeams()
})

async function loadTeams() {
  const serial = ++loadSerial
  const requestedStatus = status.value
  const requestedSearch = searchText.value.trim()
  loading.value = true
  try {
    const result = await getTeamList(requestedSearch, requestedStatus)
    if (serial === loadSerial) teams.value = result
  } catch {
    // 请求层统一提示，仅最新请求更新页面状态
  } finally {
    if (serial === loadSerial) loading.value = false
  }
}

function changeStatus(value: number) {
  status.value = value
  loadTeams()
}

function goCreate() {
  uni.navigateTo({ url: '/subpages/team/add' })
}
</script>

<style scoped>
.team-page {
  min-height: 100vh;
  padding: 36rpx 0 120rpx;
  background: #0f172a;
}

.team-header {
  padding: 0 28rpx 24rpx;
}

.team-header__title,
.team-header__subtitle {
  display: block;
}

.team-header__title {
  font-size: 44rpx;
  font-weight: 800;
}

.team-header__subtitle {
  margin-top: 8rpx;
  color: #94a3b8;
  font-size: 24rpx;
}

.team-search {
  height: 88rpx;
  margin-top: 28rpx;
  padding: 0 28rpx;
  border: 1rpx solid #334155;
  border-radius: 24rpx;
  color: #f8fafc;
  background: #1e293b;
  font-size: 26rpx;
}

.team-tabs {
  display: flex;
  margin-top: 24rpx;
  padding: 8rpx;
  border-radius: 999rpx;
  background: #1e293b;
}

.team-tabs button {
  flex: 1;
  border-radius: 999rpx;
  color: #94a3b8;
  background: transparent;
  font-size: 25rpx;
}

.team-tabs .team-tabs--active {
  color: #f8fafc;
  background: #3b82f6;
}

.team-state {
  padding: 80rpx 24rpx;
  color: #64748b;
  font-size: 26rpx;
  text-align: center;
}

.team-create {
  position: fixed;
  right: 32rpx;
  bottom: 140rpx;
  width: 104rpx;
  height: 104rpx;
  padding: 0;
  border-radius: 50%;
  color: #fff;
  background: #3b82f6;
  font-size: 54rpx;
  line-height: 96rpx;
}
</style>

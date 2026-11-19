<template>
  <view class="my-team">
    <input v-model="searchText" class="my-team__search" placeholder="搜索队伍" confirm-type="search" @confirm="loadTeams" />
    <view v-if="loading" class="my-team__state">正在加载…</view>
    <view v-else-if="!teams.length" class="my-team__state">暂无相关队伍</view>
    <TeamCardList v-else :team-list="teams" :current-user-id="userStore.userInfo?.id" @refresh="loadTeams" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TeamCardList from '@/components/TeamCardList.vue'
import { getMyCreatedTeams, getMyJoinedTeams } from '@/api/team'
import { useUserStore } from '@/store/user'
import type { TeamType } from '@/types/team'
import { requireLogin } from '@/utils/navigation'

const props = defineProps<{ mode: 'created' | 'joined' }>()
const userStore = useUserStore()
const teams = ref<TeamType[]>([])
const searchText = ref('')
const loading = ref(false)
let loadSerial = 0

onShow(() => {
  if (requireLogin()) loadTeams()
})

async function loadTeams() {
  const serial = ++loadSerial
  const requestedSearch = searchText.value.trim()
  loading.value = true
  try {
    const loader = props.mode === 'created' ? getMyCreatedTeams : getMyJoinedTeams
    const result = await loader(requestedSearch)
    if (serial === loadSerial) teams.value = result
  } catch {
    // 请求层统一提示，仅最新请求更新页面状态
  } finally {
    if (serial === loadSerial) loading.value = false
  }
}
</script>

<style scoped>
.my-team {
  min-height: 100vh;
  padding: 28rpx 0;
}

.my-team__search {
  height: 88rpx;
  margin: 0 24rpx 28rpx;
  padding: 0 28rpx;
  border-radius: 24rpx;
  color: #f8fafc;
  background: #1e293b;
}

.my-team__state {
  padding: 80rpx 24rpx;
  color: #64748b;
  text-align: center;
}
</style>

<template>
  <view class="team-page">
    <view class="team-header">
      <view class="team-header__title">找队伍</view>
      <view class="team-header__subtitle">发现并加入适合你的技术队伍</view>
    </view>
    <view class="team-search">
      <AppSearchBox v-model="searchText" placeholder="搜索队伍" @search="loadTeams" />
    </view>
    <view class="team-tabs">
      <button :class="{ 'team-tabs__button--active': status === 0 }" @click="changeStatus(0)">公开</button>
      <button :class="{ 'team-tabs__button--active': status === 2 }" @click="changeStatus(2)">加密</button>
    </view>

    <view v-if="loading" class="team-state">
      <view class="team-state__spinner" /><text>正在加载队伍...</text>
    </view>
    <TeamCardList
      v-else-if="teams.length"
      :team-list="teams"
      :current-user-id="userStore.userInfo?.id"
      @refresh="loadTeams"
    />
    <view v-else class="team-state">
      <wd-icon name="usergroup" size="64px" /><view>没有找到符合条件的队伍</view>
    </view>

    <button class="team-create" @click="goCreate"><wd-icon name="add" size="24px" /></button>
    <AppTabbar active="team" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppSearchBox from '@/components/AppSearchBox.vue'
import AppTabbar from '@/components/AppTabbar.vue'
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

onShow(() => { uni.hideTabBar({ animation: false }); if (requireLogin()) loadTeams() })
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
function changeStatus(value: number) { status.value = value; loadTeams() }
function goCreate() { uni.navigateTo({ url: '/subpages/team/add' }) }
</script>

<style scoped>
.team-page { min-height: 100vh; padding: calc(30px + var(--status-bar-height)) 0 calc(82px + env(safe-area-inset-bottom)); color: #242321; background: #faf9f6; }
.team-header { padding: 0 16px; margin-bottom: 22px; }
.team-header__title { color: #242321; font-size: 24px; font-weight: 700; line-height: 32px; letter-spacing: -.025em; }
.team-header__subtitle { margin-top: 4px; color: #66635d; font-size: 14px; line-height: 20px; }
.team-search { margin-bottom: 16px; padding: 0 16px; }
.team-search :deep(.partner-search) { margin-bottom: 0; }
.team-tabs { display: flex; gap: 6px; margin: 0 16px 20px; padding: 4px; border: 1px solid rgba(100,116,139,.2); border-radius: 16px; background: #ffffff; }
.team-tabs button { min-height: 36px; margin: 0; padding: 9px 16px; flex: 1; border: 0; border-radius: 12px; color: #85817a; background: transparent; font-size: 14px; font-weight: 500; line-height: 18px; }
.team-tabs button::after { border: 0; }
.team-tabs button:active { transform: scale(.98); }
.team-tabs .team-tabs__button--active { color: #242321; background: #eae7e0; box-shadow: 0 3px 8px rgba(2,6,23,.16); }
.team-state { display: flex; min-height: 260px; padding: 40px 20px 80px; flex-direction: column; align-items: center; justify-content: center; gap: 16px; color: #85817a; font-size: 14px; text-align: center; }
.team-state__spinner { width: 28px; height: 28px; border: 2px solid #eae7e0; border-top-color: #bd9226; border-radius: 50%; animation: spin .8s linear infinite; }
.team-create { position: fixed; right: 24px; bottom: calc(80px + env(safe-area-inset-bottom)); z-index: 20; display: grid; width: 56px; height: 56px; margin: 0; padding: 0; place-items: center; border: 0; border-radius: 50%; color: #242321; background: #bd9226; box-shadow: 0 8px 22px rgba(59,130,246,.34); }
.team-create::after { border: 0; }
.team-create:active { transform: scale(.9); }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.team-header__title { color: #242321; font-size: 30px; }
.team-header__subtitle { color: #77736c; font-size: 13px; }
.team-tabs { border-color: #eae7e0; background: #fff; }
.team-tabs button { color: #77736c; }
.team-tabs .team-tabs__button--active { color: #fff; background: #242321; box-shadow: 0 4px 13px rgba(39,34,27,.14); }
.team-create { bottom: calc(92px + env(safe-area-inset-bottom)); color: #fff; background: #242321; box-shadow: 0 10px 22px rgba(39,34,27,.18); }
.team-create:active { background: #49443f; }
</style>

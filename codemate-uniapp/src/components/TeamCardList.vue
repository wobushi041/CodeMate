<template>
  <view class="team-card-list">
    <view v-for="team in teamList" :key="team.id" class="team-card">
      <view class="team-card__header">
        <view class="team-card__heading">
          <view class="team-card__name">{{ team.name }}</view>
          <view class="team-card__description">{{ team.description || '暂无队伍介绍' }}</view>
        </view>
        <text class="team-status" :class="{ 'team-status--locked': team.status === 2 }">{{ TEAM_STATUS_TEXT[team.status] || '未知' }}</text>
      </view>

      <view class="team-card__details">
        <view class="team-detail-row">
          <wd-icon name="usergroup" size="16px" />
          <text>队伍人数: <text class="team-detail-strong">{{ memberCount(team) }}/{{ team.maxNum }}</text></text>
        </view>
        <view class="team-detail-row">
          <wd-icon name="time" size="16px" />
          <text>过期时间: <text class="team-detail-time">{{ formatDateTime(team.expireTime) }}</text></text>
        </view>
        <view class="team-detail-row">
          <wd-icon name="calendar" size="16px" />
          <text>创建时间: <text class="team-detail-time">{{ formatDateTime(team.createTime) }}</text></text>
        </view>
      </view>

      <view class="team-actions">
        <button v-if="!isOwner(team) && !team.hasJoin" class="team-action team-action--secondary" @click="preJoin(team)">
          <wd-icon name="login" size="16px" /><text>加入队伍</text>
        </button>
        <button v-if="isOwner(team)" class="team-action team-action--secondary" @click="goUpdate(team.id)">
          <wd-icon name="refresh" size="16px" /><text>更新队伍</text>
        </button>
        <button v-if="isOwner(team) || team.hasJoin" class="team-action team-action--primary" @click="goChat(team)">
          <wd-icon name="chat" size="16px" /><text>聊天室</text>
        </button>
        <button v-if="!isOwner(team) && team.hasJoin" class="team-action team-action--secondary" @click="quit(team)">
          <wd-icon name="logout" size="16px" /><text>退出队伍</text>
        </button>
        <button v-if="isOwner(team)" class="team-action team-action--secondary" @click="remove(team)">
          <wd-icon name="user-clear" size="16px" /><text>解散队伍</text>
        </button>
      </view>
    </view>

    <view v-if="passwordVisible" class="team-popup-mask" @click="closePassword">
      <view class="team-password-sheet" @click.stop>
        <view class="team-password-title">加入加密队伍</view>
        <view class="team-password-subtitle">请输入队伍密码后继续</view>
        <input v-model="password" class="team-password-input" type="password" placeholder="请输入密码" />
        <view class="team-password-actions">
          <button class="team-action team-action--secondary" @click="closePassword">取消</button>
          <button class="team-action team-action--primary" :disabled="joining" @click="confirmJoin">{{ joining ? '加入中...' : '加入队伍' }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { deleteTeam, joinTeam, quitTeam } from '@/api/team'
import { TEAM_STATUS_TEXT, type TeamType } from '@/types/team'
import { formatDateTime } from '@/utils/format'

const props = withDefaults(defineProps<{
  teamList: TeamType[]
  currentUserId?: number
}>(), {
  teamList: () => [],
  currentUserId: 0,
})

const emit = defineEmits<{ (event: 'refresh'): void }>()
const passwordVisible = ref(false)
const password = ref('')
const joinTeamId = ref(0)
const joining = ref(false)

function isOwner(team: TeamType): boolean {
  return Number(team.userId) === Number(props.currentUserId)
}

function memberCount(team: TeamType): number {
  if (typeof team.hasJoinNum === 'number') return team.hasJoinNum
  return isOwner(team) || team.hasJoin ? 1 : 0
}

function preJoin(team: TeamType) {
  joinTeamId.value = team.id
  password.value = ''
  if (team.status === 0) confirmJoin()
  else passwordVisible.value = true
}

function closePassword() {
  if (joining.value) return
  passwordVisible.value = false
  joinTeamId.value = 0
  password.value = ''
}

async function confirmJoin() {
  if (!joinTeamId.value || joining.value) return
  joining.value = true
  try {
    await joinTeam(joinTeamId.value, password.value)
    uni.showToast({ title: '加入成功', icon: 'success' })
    passwordVisible.value = false
    joinTeamId.value = 0
    password.value = ''
    emit('refresh')
  } catch {
    // 请求层已提示
  } finally {
    joining.value = false
  }
}

async function quit(team: TeamType) {
  const result = await uni.showModal({ title: '退出队伍', content: `确定退出“${team.name}”吗？` })
  if (!result.confirm) return
  try {
    await quitTeam(team.id)
    uni.showToast({ title: '已退出', icon: 'success' })
    emit('refresh')
  } catch {
    // 请求层已提示
  }
}

async function remove(team: TeamType) {
  const result = await uni.showModal({ title: '解散队伍', content: '解散后无法恢复，确定继续吗？', confirmColor: '#ad4a46' })
  if (!result.confirm) return
  try {
    await deleteTeam(team.id)
    uni.showToast({ title: '已解散', icon: 'success' })
    emit('refresh')
  } catch {
    // 请求层已提示
  }
}

function goUpdate(id: number) {
  uni.navigateTo({ url: `/subpages/team/update?id=${id}` })
}

function goChat(team: TeamType) {
  uni.navigateTo({ url: `/subpages/chat/team?teamId=${team.id}&teamName=${encodeURIComponent(team.name)}` })
}
</script>

<style scoped>
.team-card-list { display: grid; gap: 14px; padding: 0 12px 18px; }
.team-card { padding: 17px 15px 16px; overflow: hidden; border: 1px solid rgba(100,116,139,.32); border-radius: 18px; background: #ffffff; box-shadow: 0 8px 20px rgba(2,6,23,.08); }
.team-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; padding-bottom: 14px; }
.team-card__heading { min-width: 0; flex: 1; }
.team-card__name { overflow: hidden; color: #242321; font-size: 17px; font-weight: 700; line-height: 22px; text-overflow: ellipsis; white-space: nowrap; }
.team-card__description { margin-top: 4px; overflow: hidden; color: #66635d; font-size: 12px; line-height: 18px; text-overflow: ellipsis; white-space: nowrap; }
.team-status { flex: 0 0 auto; padding: 4px 10px; border-radius: 999px; color: #bd9226; background: rgba(59,130,246,.12); font-size: 11px; font-weight: 500; line-height: 18px; white-space: nowrap; }
.team-status--locked { color: #66635d; background: #eae7e0; }
.team-card__details { display: grid; gap: 9px; padding: 13px 0 15px; border-top: 1px solid rgba(100,116,139,.25); color: #66635d; }
.team-detail-row { display: flex; min-width: 0; align-items: center; gap: 8px; font-size: 12px; line-height: 18px; }
.team-detail-row wd-icon { flex: 0 0 auto; color: #85817a; }
.team-detail-strong { color: #242321; font-weight: 600; }
.team-detail-time { color: #242321; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 10px; }
.team-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; padding-top: 13px; border-top: 1px solid rgba(100,116,139,.25); }
.team-action { display: inline-flex; min-height: 32px; margin: 0; padding: 7px 13px; align-items: center; justify-content: center; gap: 6px; border: 0; border-radius: 999px; font-size: 12px; font-weight: 500; line-height: 18px; white-space: nowrap; box-shadow: 0 2px 5px rgba(2,6,23,.12); }
.team-action::after { border: 0; }
.team-action:active { transform: scale(.95); }
.team-action--primary { color: #242321; background: #fff; }
.team-action--secondary { color: #242321; background: #eae7e0; }
.team-popup-mask { position: fixed; z-index: 1000; display: flex; align-items: flex-end; background: rgba(2,6,23,.62); inset: 0; }
.team-password-sheet { width: 100%; padding: 20px 16px calc(18px + env(safe-area-inset-bottom)); border-radius: 18px 18px 0 0; background: #ffffff; }
.team-password-title { color: #242321; font-size: 18px; font-weight: 600; line-height: 26px; text-align: center; }
.team-password-subtitle { margin-top: 4px; color: #85817a; font-size: 13px; line-height: 20px; text-align: center; }
.team-password-input { height: 46px; margin-top: 18px; padding: 0 16px; border: 1px solid rgba(148,163,184,.18); border-radius: 12px; color: #242321; background: #faf9f6; font-size: 14px; }
.team-password-actions { display: flex; width: 100%; gap: 12px; margin-top: 18px; }
.team-password-actions .team-action { height: 44px; flex: 1 1 0; }
.team-card { border: 1px solid #eae7e0; border-radius: 21px; background: #fff; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.team-card__name { color: #242321; font-size: 18px; }
.team-card__description, .team-card__details { color: #77736c; }
.team-status { color: #927029; background: #f8f0dd; }
.team-status--locked { color: #77736c; background: #f1efea; }
.team-card__details, .team-actions { border-color: #eae7e0; }
.team-detail-row wd-icon { color: #bd9226; }
.team-detail-strong, .team-detail-time { color: #242321; }
.team-action { box-shadow: none; }
.team-action--primary { color: #fff; background: #242321; }
.team-action--secondary { border: 1px solid #eae7e0; color: #242321; background: #fff; }
.team-password-sheet { background: #fff; }
.team-password-title { color: #242321; }
.team-password-subtitle { color: #77736c; }
.team-password-input { border-color: #eae7e0; color: #242321; background: #faf9f6; }
</style>

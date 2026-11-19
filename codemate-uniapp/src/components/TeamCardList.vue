<template>
  <view class="team-list">
    <view v-for="team in teamList" :key="team.id" class="team-card">
      <view class="team-card__header">
        <view class="team-card__heading">
          <text class="team-card__name">{{ team.name }}</text>
          <text class="team-card__description">{{ team.description || '暂无队伍介绍' }}</text>
        </view>
        <text class="team-card__status">{{ TEAM_STATUS_TEXT[team.status] || '未知' }}</text>
      </view>

      <view class="team-card__details">
        <text>队伍人数：{{ memberCount(team) }}/{{ team.maxNum }}</text>
        <text>过期时间：{{ formatDateTime(team.expireTime) }}</text>
        <text>创建时间：{{ formatDateTime(team.createTime) }}</text>
      </view>

      <view class="team-card__actions">
        <button v-if="!isOwner(team) && !team.hasJoin" size="mini" @click="preJoin(team)">加入队伍</button>
        <button v-if="isOwner(team)" size="mini" @click="goUpdate(team.id)">更新队伍</button>
        <button v-if="isOwner(team) || team.hasJoin" size="mini" class="team-card__primary" @click="goChat(team)">聊天室</button>
        <button v-if="!isOwner(team) && team.hasJoin" size="mini" @click="quit(team)">退出队伍</button>
        <button v-if="isOwner(team)" size="mini" class="team-card__danger" @click="remove(team)">解散队伍</button>
      </view>
    </view>

    <view v-if="passwordVisible" class="password-mask" @click="closePassword">
      <view class="password-panel" @click.stop>
        <text class="password-panel__title">加入加密队伍</text>
        <input v-model="password" class="password-panel__input" type="password" placeholder="请输入队伍密码" />
        <view class="password-panel__actions">
          <button size="mini" @click="closePassword">取消</button>
          <button size="mini" class="team-card__primary" :loading="joining" @click="confirmJoin">确认加入</button>
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
  if (team.status === 0) {
    confirmJoin()
  } else {
    passwordVisible.value = true
  }
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
  const result = await uni.showModal({ title: '解散队伍', content: '解散后无法恢复，确定继续吗？', confirmColor: '#ef4444' })
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
  uni.navigateTo({
    url: `/subpages/chat/team?teamId=${team.id}&teamName=${encodeURIComponent(team.name)}`,
  })
}
</script>

<style scoped>
.team-card {
  margin: 0 24rpx 24rpx;
  padding: 28rpx;
  border: 1rpx solid #334155;
  border-radius: 28rpx;
  background: #1e293b;
}

.team-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.team-card__heading {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.team-card__name {
  overflow: hidden;
  font-size: 32rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-card__description {
  overflow: hidden;
  margin-top: 10rpx;
  color: #94a3b8;
  font-size: 24rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-card__status {
  margin-left: 16rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  color: #93c5fd;
  background: #172554;
  font-size: 22rpx;
}

.team-card__details {
  display: flex;
  margin-top: 24rpx;
  padding: 20rpx 0;
  flex-direction: column;
  gap: 12rpx;
  border-top: 1rpx solid #334155;
  border-bottom: 1rpx solid #334155;
  color: #cbd5e1;
  font-size: 23rpx;
}

.team-card__actions {
  display: flex;
  margin-top: 20rpx;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12rpx;
}

.team-card__actions button {
  margin: 0;
  color: #f8fafc;
  background: #334155;
  font-size: 23rpx;
}

.team-card__primary {
  color: #0f172a !important;
  background: #f8fafc !important;
}

.team-card__danger {
  color: #fecaca !important;
}

.password-mask {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: flex;
  padding: 48rpx;
  align-items: center;
  background: rgba(0, 0, 0, 0.65);
}

.password-panel {
  display: flex;
  width: 100%;
  padding: 36rpx;
  flex-direction: column;
  border-radius: 28rpx;
  background: #1e293b;
}

.password-panel__title {
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
}

.password-panel__input {
  height: 88rpx;
  margin-top: 28rpx;
  padding: 0 24rpx;
  border-radius: 16rpx;
  background: #0f172a;
}

.password-panel__actions {
  display: flex;
  margin-top: 28rpx;
  justify-content: space-between;
}
</style>

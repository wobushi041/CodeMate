<template>
  <view v-if="loading" class="team-update__state">正在加载队伍信息…</view>
  <TeamForm
    v-else
    :initial="team"
    :show-max-num="false"
    submit-text="保存修改"
    :submitting="submitting"
    @submit="submit"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import TeamForm from '@/components/TeamForm.vue'
import { getTeamDetail, updateTeam } from '@/api/team'
import type { TeamFormParams, TeamType } from '@/types/team'
import { requireLogin } from '@/utils/navigation'

const team = ref<TeamType | null>(null)
const loading = ref(false)
const submitting = ref(false)
const teamId = ref(0)

onLoad((options) => {
  if (!requireLogin()) return
  teamId.value = Number(options?.id) || 0
  if (!teamId.value) {
    uni.showToast({ title: '缺少队伍 ID', icon: 'none' })
    return
  }
  loadTeam()
})

async function loadTeam() {
  loading.value = true
  try {
    team.value = await getTeamDetail(teamId.value)
  } catch {
    // 请求层统一提示
  } finally {
    loading.value = false
  }
}

async function submit(data: TeamFormParams) {
  if (submitting.value) return
  submitting.value = true
  try {
    await updateTeam({ ...data, id: teamId.value })
    uni.showToast({ title: '更新成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 300)
  } catch {
    // 请求层统一提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.team-update__state {
  padding: 100rpx 32rpx;
  color: #85817a;
  text-align: center;
}
</style>

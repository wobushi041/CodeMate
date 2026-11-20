<template>
  <TeamForm submit-text="提交" :submitting="submitting" @submit="submit" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TeamForm from '@/components/TeamForm.vue'
import { createTeam } from '@/api/team'
import type { TeamFormParams } from '@/types/team'
import { requireLogin } from '@/utils/navigation'
import { onLoad } from '@dcloudio/uni-app'

const submitting = ref(false)
onLoad(() => requireLogin())

async function submit(data: TeamFormParams) {
  if (submitting.value) return
  submitting.value = true
  try {
    await createTeam(data)
    uni.showToast({ title: '创建成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 300)
  } catch {
    // 请求层统一提示
  } finally {
    submitting.value = false
  }
}
</script>

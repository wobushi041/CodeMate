<template>
  <ChatRoom
    v-if="teamId"
    ref="roomRef"
    mode="team"
    :conversation-id="teamId"
  />
  <view v-else class="chat-page__state">缺少队伍 ID</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import ChatRoom from '@/components/ChatRoom.vue'
import { requireLogin } from '@/utils/navigation'
import { decodeQueryValue } from '@/utils/query'

interface ChatRoomExpose {
  reconnect: () => void
  close: () => void
  loadHistory: () => Promise<void>
}

const roomRef = ref<ChatRoomExpose | null>(null)
const teamId = ref(0)

onLoad((options) => {
  if (!requireLogin()) return
  teamId.value = Number(options?.teamId) || 0
  uni.setNavigationBarTitle({ title: decodeQueryValue(options?.teamName) || '队伍聊天室' })
})

onShow(() => roomRef.value?.reconnect())
onHide(() => roomRef.value?.close())
</script>

<style scoped>
.chat-page__state {
  padding: 100rpx 32rpx;
  color: #94a3b8;
  text-align: center;
}
</style>

<template>
  <ChatRoom
    v-if="sessionId"
    ref="roomRef"
    mode="private"
    :conversation-id="sessionId"
    :target-avatar="targetAvatar"
    :target-online="targetOnline"
  />
  <view v-else class="chat-page__state">{{ errorText || '正在初始化私聊…' }}</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import ChatRoom from '@/components/ChatRoom.vue'
import { startPrivateChat } from '@/api/chat'
import { markUserContacted } from '@/utils/contacted'
import { requireLogin } from '@/utils/navigation'
import { decodeQueryValue } from '@/utils/query'

interface ChatRoomExpose {
  reconnect: () => void
  close: () => void
  loadHistory: () => Promise<void>
}

const roomRef = ref<ChatRoomExpose | null>(null)
const sessionId = ref(0)
const targetUserId = ref(0)
const targetAvatar = ref('')
const targetOnline = ref(false)
const errorText = ref('')

onLoad(async (options) => {
  if (!requireLogin()) return
  sessionId.value = Number(options?.sessionId) || 0
  targetUserId.value = Number(options?.targetUserId) || 0
  targetAvatar.value = decodeQueryValue(options?.targetAvatarUrl)
  targetOnline.value = options?.isOnline === '1' || options?.isOnline === 'true'
  const title = decodeQueryValue(options?.targetUsername) || '私聊伙伴'
  uni.setNavigationBarTitle({ title })
  markUserContacted(targetUserId.value)

  if (!sessionId.value && targetUserId.value) {
    try {
      const session = await startPrivateChat(targetUserId.value)
      sessionId.value = Number(session.sessionId)
      targetOnline.value = Boolean(session.isTargetOnline)
      targetAvatar.value = session.targetUser?.avatarUrl || targetAvatar.value
    } catch {
      errorText.value = '私聊会话初始化失败'
    }
  }
  if (!sessionId.value && !errorText.value) errorText.value = '缺少私聊会话参数'
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

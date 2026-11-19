<template>
  <view class="chat-room">
    <view class="chat-room__status">{{ statusText }}</view>

    <scroll-view
      class="chat-room__messages"
      scroll-y
      :scroll-into-view="scrollIntoView"
      :scroll-with-animation="true"
    >
      <view v-if="loading && !messages.length" class="chat-room__empty">正在加载聊天记录…</view>
      <view v-else-if="!messages.length" class="chat-room__empty">暂无消息，发送第一条消息吧</view>

      <view
        v-for="(message, index) in messages"
        :id="messageDomId(message, index)"
        :key="messageKey(message, index)"
        class="chat-message"
        :class="{ 'chat-message--self': isSelf(message) }"
      >
        <image v-if="avatarOf(message)" class="chat-message__avatar" :src="avatarOf(message)" mode="aspectFill" />
        <view v-else class="chat-message__avatar chat-message__avatar--fallback">{{ senderName(message).slice(0, 1) }}</view>
        <view class="chat-message__body">
          <view class="chat-message__meta">
            <text>{{ senderName(message) }}</text>
            <text>{{ formatTime(message.createTime) }}</text>
          </view>
          <view class="chat-message__bubble">{{ message.content || message.message }}</view>
        </view>
      </view>
    </scroll-view>

    <view class="chat-room__composer">
      <input
        v-model="inputContent"
        class="chat-room__input"
        placeholder="输入消息…"
        confirm-type="send"
        :maxlength="2048"
        :disabled="sending"
        @confirm="sendMessage"
      />
      <button :disabled="!canSend" @click="sendMessage">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { getPrivateMessages, getTeamMessages } from '@/api/chat'
import { ChatSocket, type ChatSocketStatus } from '@/realtime/chatSocket'
import { useUserStore } from '@/store/user'
import type { ChatMessage } from '@/types/chat'
import { formatTime } from '@/utils/format'

const props = withDefaults(defineProps<{
  mode: 'private' | 'team'
  conversationId: number
  targetAvatar?: string
  targetOnline?: boolean
}>(), {
  targetAvatar: '',
  targetOnline: false,
})

const userStore = useUserStore()
const messages = ref<ChatMessage[]>([])
const inputContent = ref('')
const loading = ref(false)
const sending = ref(false)
const socketStatus = ref<ChatSocketStatus>('connecting')
const joined = ref(false)
const scrollIntoView = ref('')
let socket: ChatSocket | null = null

const statusText = computed(() => {
  if (joined.value) {
    if (props.mode === 'private') return props.targetOnline ? '对方在线' : '对方离线，可留言'
    return '已进入聊天室'
  }
  if (socketStatus.value === 'reconnecting') return '连接中断，正在重连…'
  if (socketStatus.value === 'closed') return '连接已关闭'
  return '正在连接…'
})
const canSend = computed(() => joined.value && !sending.value && inputContent.value.trim().length > 0)

onMounted(async () => {
  await loadHistory()
  connect()
})

onUnmounted(() => close())

async function loadHistory() {
  if (!props.conversationId || loading.value) return
  loading.value = true
  try {
    const page = props.mode === 'private'
      ? await getPrivateMessages(props.conversationId)
      : await getTeamMessages(props.conversationId)
    const history = [...(page.records || [])].reverse()
    const merged = [...history]
    messages.value.forEach((message) => {
      if (!history.some((item) => isSameMessage(item, message))) merged.push(message)
    })
    messages.value = merged.sort((left, right) => messageTimestamp(left) - messageTimestamp(right))
    scrollToBottom()
  } catch {
    // 请求层统一提示
  } finally {
    loading.value = false
  }
}

function connect() {
  if (socket) return
  const joinMessage = props.mode === 'private'
    ? { type: 'PRIVATE_JOIN', sessionId: props.conversationId }
    : { type: 'JOIN', teamId: props.conversationId }

  socket = new ChatSocket({
    joinMessage,
    onStatus: (status) => {
      socketStatus.value = status
      if (status !== 'connected') joined.value = false
    },
    onMessage: (data) => {
      const joinedType = props.mode === 'private' ? 'PRIVATE_JOINED' : 'JOINED'
      const chatType = props.mode === 'private' ? 'PRIVATE_CHAT' : 'CHAT'
      if (data.type === joinedType) {
        joined.value = true
        return
      }
      if (data.type === chatType) {
        appendMessage(data)
        return
      }
      if (data.type === 'ERROR') {
        uni.showToast({ title: data.message || '聊天室错误', icon: 'none' })
      }
    },
  })
  socket.connect()
}

async function sendMessage() {
  const content = inputContent.value.trim()
  if (!content || !socket || !joined.value || sending.value) return
  const clientMessageId = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  sending.value = true
  try {
    const sent = await socket.send({
      type: props.mode === 'private' ? 'PRIVATE_CHAT' : 'CHAT',
      clientMessageId,
      content,
    })
    if (sent) {
      if (inputContent.value.trim() === content) inputContent.value = ''
    } else {
      uni.showToast({ title: '消息发送失败，请重试', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '消息发送失败，请重试', icon: 'none' })
  } finally {
    sending.value = false
  }
}

function isSameMessage(left: ChatMessage, right: ChatMessage): boolean {
  return Boolean(
    (left.messageId && left.messageId === right.messageId)
    || (left.clientMessageId && left.clientMessageId === right.clientMessageId),
  )
}

function messageTimestamp(message: ChatMessage): number {
  if (!message.createTime) return Number.MAX_SAFE_INTEGER
  const timestamp = new Date(message.createTime).getTime()
  return Number.isNaN(timestamp) ? Number.MAX_SAFE_INTEGER : timestamp
}

function appendMessage(message: ChatMessage) {
  const exists = messages.value.some((item) => isSameMessage(item, message))
  if (!exists) messages.value.push(message)
  scrollToBottom()
}

function isSelf(message: ChatMessage): boolean {
  return Number(message.fromUserId) === Number(userStore.userInfo?.id)
}

function senderName(message: ChatMessage): string {
  if (isSelf(message)) return userStore.userInfo?.username || '我'
  return message.fromUsername || (props.mode === 'private' ? '伙伴' : '队伍成员')
}

function avatarOf(message: ChatMessage): string {
  if (isSelf(message)) return userStore.userInfo?.avatarUrl || ''
  return message.fromAvatarUrl || props.targetAvatar || ''
}

function messageKey(message: ChatMessage, index: number): string {
  return String(message.messageId || message.clientMessageId || `${message.createTime}-${index}`)
}

function messageDomId(message: ChatMessage, index: number): string {
  return `message-${messageKey(message, index)}`
}

function scrollToBottom() {
  nextTick(() => {
    if (!messages.value.length) return
    scrollIntoView.value = ''
    nextTick(() => {
      scrollIntoView.value = messageDomId(messages.value[messages.value.length - 1], messages.value.length - 1)
    })
  })
}

function reconnect() {
  if (socket) socket.reconnect()
  else connect()
  void loadHistory()
}

function close() {
  socket?.close()
  socket = null
  joined.value = false
}

defineExpose({ reconnect, close, loadHistory })
</script>

<style scoped>
.chat-room {
  display: flex;
  height: 100vh;
  flex-direction: column;
  background: #0f172a;
}

.chat-room__status {
  padding: 14rpx 24rpx;
  color: #94a3b8;
  background: #1e293b;
  font-size: 22rpx;
  text-align: center;
}

.chat-room__messages {
  min-height: 0;
  padding: 24rpx;
  flex: 1;
}

.chat-room__empty {
  padding: 100rpx 24rpx;
  color: #64748b;
  font-size: 25rpx;
  text-align: center;
}

.chat-message {
  display: flex;
  margin-bottom: 28rpx;
  align-items: flex-start;
}

.chat-message--self {
  flex-direction: row-reverse;
}

.chat-message__avatar {
  display: flex;
  width: 72rpx;
  height: 72rpx;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #334155;
}

.chat-message__avatar--fallback {
  color: #f8fafc;
  font-size: 28rpx;
}

.chat-message__body {
  max-width: 72%;
  margin: 0 18rpx;
}

.chat-message__meta {
  display: flex;
  margin-bottom: 8rpx;
  justify-content: space-between;
  gap: 20rpx;
  color: #64748b;
  font-size: 20rpx;
}

.chat-message--self .chat-message__meta {
  flex-direction: row-reverse;
}

.chat-message__bubble {
  padding: 18rpx 22rpx;
  border-radius: 8rpx 24rpx 24rpx 24rpx;
  color: #f8fafc;
  background: #1e293b;
  font-size: 27rpx;
  line-height: 1.6;
  word-break: break-word;
}

.chat-message--self .chat-message__bubble {
  border-radius: 24rpx 8rpx 24rpx 24rpx;
  color: #0f172a;
  background: #f8fafc;
}

.chat-room__composer {
  display: flex;
  padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom));
  gap: 16rpx;
  border-top: 1rpx solid #334155;
  background: #1e293b;
}

.chat-room__input {
  height: 80rpx;
  padding: 0 24rpx;
  flex: 1;
  border-radius: 20rpx;
  color: #f8fafc;
  background: #0f172a;
  font-size: 26rpx;
}

.chat-room__composer button {
  width: 120rpx;
  height: 80rpx;
  padding: 0;
  border-radius: 20rpx;
  color: #0f172a;
  background: #f8fafc;
  font-size: 26rpx;
  line-height: 80rpx;
}
</style>

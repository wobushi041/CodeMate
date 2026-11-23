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
      ...(props.mode === 'private' ? { sessionId: props.conversationId } : {}),
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
.chat-room { display: flex; height: 100vh; min-height: 0; flex-direction: column; overflow: hidden; color: #242321; background: #faf9f6; }
.chat-room__status { flex: 0 0 auto; padding: 6px 16px; color: #85817a; background: #faf9f6; font-size: 11px; line-height: 16px; text-align: center; }
.chat-room__messages { min-height: 0; padding: 16px; flex: 1; }
.chat-room__empty { padding: 72px 20px; color: #85817a; font-size: 13px; line-height: 20px; text-align: center; }
.chat-message { display: flex; width: 100%; margin-bottom: 24px; align-items: flex-start; gap: 12px; }
.chat-message--self { flex-direction: row-reverse; }
.chat-message__avatar { display: flex; width: 40px; height: 40px; flex: 0 0 40px; align-items: center; justify-content: center; border: 2px solid rgba(255,255,255,.7); border-radius: 50%; background: #eae7e0; }
.chat-message__avatar--fallback { color: #242321; font-size: 14px; }
.chat-message__body { display: flex; min-width: 0; max-width: calc(100% - 60px); flex-direction: column; }
.chat-message--self .chat-message__body { align-items: flex-end; }
.chat-message__meta { display: flex; margin-bottom: 6px; align-items: center; gap: 10px; color: #85817a; font-size: 11px; line-height: 16px; }
.chat-message--self .chat-message__meta { flex-direction: row-reverse; }
.chat-message__bubble { max-width: 100%; padding: 12px 18px; border: 1px solid rgba(51,65,85,.5); border-radius: 18px 18px 18px 0; color: #242321; background: #ffffff; box-shadow: 0 8px 20px rgba(2,6,23,.2); font-size: 14px; line-height: 1.65; overflow-wrap: break-word; }
.chat-message--self .chat-message__bubble { border: 0; border-radius: 18px 18px 0 18px; color: #242321; background: #fff; }
.chat-room__composer { display: flex; margin: 12px 16px calc(14px + env(safe-area-inset-bottom)); padding: 8px; flex: 0 0 auto; align-items: center; gap: 10px; border: 1px solid rgba(255,255,255,.1); border-radius: 999px; background: rgba(255,255,255,.05); box-shadow: 0 14px 30px rgba(2,6,23,.3); }
.chat-room__input { min-width: 0; height: 44px; padding: 0 18px; flex: 1; border: 1px solid rgba(255,255,255,.05); border-radius: 999px; color: #242321; background: rgba(15,23,42,.65); font-size: 14px; }
.chat-room__composer button { display: grid; width: 52px; height: 44px; margin: 0; padding: 0; flex: 0 0 52px; place-items: center; border: 0; border-radius: 50%; color: #fff; background: #bd9226; font-size: 13px; line-height: 44px; }
.chat-room__composer button::after { border: 0; }
.chat-room__composer button:disabled, .chat-room__composer button[disabled] { color: #fff; background: #bd9226; opacity: .45; }
.chat-room { color: #242321; background: #f7f6f3; }
.chat-room__status { color: #77736c; background: #f7f6f3; }
.chat-room__empty, .chat-message__meta { color: #77736c; }
.chat-message__avatar { border-color: #fff; color: #242321; background: #eeeae2; }
.chat-message__bubble { border-color: #eae7e0; color: #242321; background: #fff; box-shadow: 0 8px 22px rgba(39,34,27,.06); }
.chat-message--self .chat-message__bubble { border: 1px solid #242321; color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.chat-room__composer { gap: 8px; padding: 7px; border-color: rgba(255,255,255,.84); background: rgba(255,255,255,.56); box-shadow: 0 10px 30px rgba(39,34,27,.1); }
.chat-room__input { height: 44px; padding: 0 16px; border-color: transparent; color: #242321; background: transparent; }
.chat-room__composer button, .chat-room__composer button:disabled, .chat-room__composer button[disabled] { width: 44px; height: 44px; flex-basis: 44px; color: #fff; background: #242321; }
</style>

<template>
  <view class="ai-page">
    <view class="ai-header"><view>AI 编程助手</view></view>
    <scroll-view class="message-list" scroll-y :scroll-into-view="scrollIntoView" :scroll-with-animation="true">
      <view
        v-for="(message, index) in messages"
        :id="`ai-message-${index}`"
        :key="message.id"
        class="message-row"
        :class="message.role === 'user' ? 'message-row--user' : 'message-row--ai'"
      >
        <view v-if="message.role === 'ai'" class="ai-message-wrap">
          <view class="ai-message-icon"><wd-icon name="chat" size="30px" /></view>
          <view class="message-bubble ai-message-bubble" :class="{ 'message-bubble--loading': isStreaming(message, index) }">
            <wd-icon v-if="isStreaming(message, index)" name="refresh" class="loading-icon" size="22px" />
            <MarkdownText v-else class="message-text" :content="message.content" />
          </view>
          <text v-if="!isStreaming(message, index)" class="message-time message-time--ai">{{ formatMessageTime(message.createdAt) }}</text>
        </view>
        <template v-else>
          <view class="user-message-content">
            <view class="message-bubble user-message-bubble"><MarkdownText class="message-text" :content="message.content" /></view>
            <text class="message-time message-time--user">{{ formatMessageTime(message.createdAt) }}</text>
          </view>
          <image class="user-avatar" :src="userAvatar" mode="aspectFill" />
        </template>
      </view>
    </scroll-view>

    <view v-if="errorText" class="ai-error">{{ errorText }}</view>
    <view class="ai-input-dock">
      <view class="input-shell">
        <input v-model="input" class="message-input" type="text" placeholder="输入你的问题..." confirm-type="send" :disabled="loading" @confirm="send" />
        <button class="send-button" :disabled="loading || !input.trim()" @click="send">
          <wd-icon v-if="loading" name="refresh" class="loading-icon" size="21px" />
          <wd-icon v-else name="jump" size="21px" />
        </button>
      </view>
    </view>
    <AppTabbar active="ai" />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabbar from '@/components/AppTabbar.vue'
import MarkdownText from '@/components/MarkdownText.vue'
import { streamAiChat } from '@/realtime/aiStream'
import { useUserStore } from '@/store/user'
import { requireLogin } from '@/utils/navigation'

interface AiMessage { id: string; role: 'user' | 'ai'; content: string; createdAt: number }
const userStore = useUserStore()
const messages = ref<AiMessage[]>([])
const input = ref('')
const loading = ref(false)
const errorText = ref('')
const scrollIntoView = ref('')
const userAvatar = computed(() => userStore.userInfo?.avatarUrl || 'https://api.dicebear.com/8.x/initials/svg?seed=Code&backgroundColor=334155&fontFamily=Arial')
let cancelStream: (() => void) | null = null
let revealTimer: ReturnType<typeof setInterval> | null = null
let revealQueue = ''
let activeAiIndex = -1
let hasAutoGreeted = false

onShow(async () => {
  uni.hideTabBar({ animation: false })
  if (!requireLogin()) return
  if (!userStore.userInfo) await userStore.fetchCurrentUser().catch(() => undefined)
  if (!hasAutoGreeted && !messages.value.length && !loading.value) {
    hasAutoGreeted = true
    sendAndStream('你好，请简短介绍一下你能帮我什么', true)
  }
})
onUnmounted(() => stop())
function isStreaming(message: AiMessage, index: number) { return loading.value && message.role === 'ai' && index === messages.value.length - 1 && !message.content }
function formatMessageTime(timestamp: number) { const date = new Date(timestamp); return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}` }

function send() {
  const content = input.value.trim()
  if (!content || loading.value) return
  input.value = ''
  sendAndStream(content)
}

function sendAndStream(content: string, silent = false) {
  if (!content || loading.value) return
  errorText.value = ''
  const now = Date.now()
  if (!silent) messages.value.push({ id: `user-${now}`, role: 'user', content, createdAt: now })
  messages.value.push({ id: `ai-${now}`, role: 'ai', content: '', createdAt: now })
  activeAiIndex = messages.value.length - 1
  loading.value = true
  scrollToBottom()
  cancelStream = streamAiChat(content, {
    onText: (text) => { revealQueue += text; startReveal() },
    onError: (message) => {
      flushReveal()
      errorText.value = message || 'AI 回复失败，请稍后重试'
      if (!messages.value[activeAiIndex]?.content) messages.value[activeAiIndex].content = '本次回复失败，请重新发送问题。'
      finish()
    },
    onComplete: () => { flushReveal(); finish() },
  })
}
function startReveal() {
  if (revealTimer) return
  revealTimer = setInterval(() => {
    if (activeAiIndex < 0 || !revealQueue) { clearRevealTimer(); return }
    const count = Math.min(24, Math.max(1, Math.ceil(revealQueue.length / 60)))
    messages.value[activeAiIndex].content += revealQueue.slice(0, count)
    revealQueue = revealQueue.slice(count)
    scrollToBottom()
  }, 35)
}
function flushReveal() {
  if (activeAiIndex >= 0 && revealQueue) { messages.value[activeAiIndex].content += revealQueue; revealQueue = '' }
  clearRevealTimer(); scrollToBottom()
}
function clearRevealTimer() { if (revealTimer) clearInterval(revealTimer); revealTimer = null }
function finish() { loading.value = false; cancelStream = null; activeAiIndex = -1; revealQueue = '' }
function stop() { cancelStream?.(); cancelStream = null; flushReveal(); finish() }
function scrollToBottom() {
  nextTick(() => {
    if (!messages.value.length) return
    scrollIntoView.value = ''
    nextTick(() => { scrollIntoView.value = `ai-message-${messages.value.length - 1}` })
  })
}
</script>

<style scoped>
.ai-page { display: flex; height: calc(100vh - 64px - env(safe-area-inset-bottom)); min-height: 0; flex-direction: column; overflow: hidden; color: #242321; background: #faf9f6; }
.ai-header { display: flex; min-height: 54px; flex: 0 0 auto; align-items: center; justify-content: center; padding: calc(22px + var(--status-bar-height)) 16px 12px; color: #242321; background: #faf9f6; font-size: 20px; font-weight: 700; line-height: 28px; letter-spacing: -.025em; }
.message-list { width: 100%; min-height: 0; flex: 1; padding: 20px 16px 16px; overflow-x: hidden; }
.message-row { display: flex; width: 100%; margin-bottom: 48px; align-items: flex-start; gap: 16px; }
.message-row:last-child { margin-bottom: 0; }
.message-row--ai { justify-content: flex-start; }
.message-row--user { justify-content: flex-end; }
.ai-message-wrap { position: relative; display: flex; min-width: 0; padding-top: 8px; padding-left: 24px; flex: 1; flex-direction: column; }
.ai-message-icon { position: absolute; top: -12px; left: -12px; z-index: 2; display: grid; width: 34px; height: 34px; place-items: center; color: #bd9226; background: #faf9f6; }
.message-bubble { font-size: 14px; line-height: 1.75; overflow-wrap: break-word; }
.ai-message-bubble { position: relative; width: 100%; min-height: 64px; padding: 20px 28px 30px; border: 1px solid rgba(51,65,85,.5); border-radius: 24px 24px 24px 0; color: #242321; background: #ffffff; box-shadow: 0 10px 25px rgba(2,6,23,.3); }
.user-message-content { display: flex; min-width: 0; max-width: calc(100% - 60px); flex: 0 1 auto; flex-direction: column; align-items: flex-end; }
.user-message-bubble { max-width: 100%; padding: 14px 24px; border-radius: 24px 24px 0 24px; color: #242321; background: #fff; box-shadow: 0 10px 24px rgba(2,6,23,.24); }
.user-message-bubble :deep(.markdown-text), .user-message-bubble :deep(.markdown-text__heading) { color: #242321; }
.user-avatar { width: 44px; height: 44px; margin-top: 4px; flex: 0 0 44px; border: 2px solid #fff; border-radius: 50%; background: #eae7e0; box-shadow: 0 8px 18px rgba(2,6,23,.3); }
.message-bubble--loading { display: flex; min-width: 72px; min-height: 64px; padding-bottom: 20px; align-items: center; justify-content: center; }
.loading-icon { animation: ai-spin 1s linear infinite; }
.message-time { color: #85817a; font-family: Consolas, Monaco, monospace; font-size: 11px; line-height: 16px; }
.message-time--ai { align-self: flex-start; margin-top: 8px; padding-left: 4px; }
.message-time--user { align-self: flex-end; margin-top: 8px; padding-right: 4px; }
.message-text { font-weight: 400; }
.ai-error { flex: 0 0 auto; padding: 8px 16px; color: #fecaca; background: #451a1a; font-size: 12px; }
.ai-input-dock { flex: 0 0 auto; padding: 12px 16px 14px; background: linear-gradient(180deg,rgba(15,23,42,0),#faf9f6 24%); }
.input-shell { display: flex; padding: 10px; align-items: center; gap: 14px; border: 1px solid rgba(255,255,255,.1); border-radius: 999px; background: rgba(255,255,255,.05); box-shadow: 0 18px 40px rgba(2,6,23,.4); backdrop-filter: blur(20px); }
.message-input { min-width: 0; height: 48px; padding: 0 24px; flex: 1; border: 1px solid rgba(255,255,255,.05); border-radius: 999px; color: #242321; background: rgba(15,23,42,.65); font-size: 14px; }
.send-button { display: grid; width: 48px; height: 48px; margin: 0; padding: 0; flex: 0 0 48px; place-items: center; border: 0; border-radius: 50%; color: #fff; background: #bd9226; box-shadow: 0 8px 18px rgba(59,130,246,.3); }
.send-button::after { border: 0; }
.send-button:not(:disabled):active { transform: scale(.93); background: #49443f; }
.send-button:disabled, .send-button[disabled] { color: #fff; background: #bd9226; opacity: .5; }
@keyframes ai-spin { to { transform: rotate(360deg); } }
.ai-page, .ai-header { color: #242321; background: #f7f6f3; }
.ai-message-bubble { border-color: #eae7e0; color: #242321; background: #fff; box-shadow: 0 8px 22px rgba(39,34,27,.06); }
.user-message-bubble { border: 1px solid #242321; color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.user-message-bubble :deep(.markdown-text), .user-message-bubble :deep(.markdown-text__heading), .user-message-bubble :deep(.markdown-text__paragraph) { color: #fff; }
.user-avatar { border-color: #fff; background: #eeeae2; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.message-time { color: #77736c; font-family: inherit; }
.ai-error { color: #a34d47; background: #fff6f3; }
.ai-input-dock { background: transparent; }
.input-shell { gap: 8px; padding: 7px; border-color: rgba(255,255,255,.84); background: rgba(255,255,255,.56); box-shadow: 0 10px 30px rgba(39,34,27,.1); }
.message-input { height: 44px; padding: 0 16px; border-color: transparent; color: #242321; background: transparent; }
.send-button, .send-button:disabled, .send-button[disabled] { width: 44px; height: 44px; flex-basis: 44px; color: #fff; background: #242321; box-shadow: none; }
</style>

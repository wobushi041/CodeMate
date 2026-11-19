<template>
  <view class="ai-page">
    <scroll-view class="ai-page__messages" scroll-y :scroll-into-view="scrollIntoView" :scroll-with-animation="true">
      <view v-if="!messages.length" class="ai-welcome">
        <text class="ai-welcome__title">AI 编程助手</text>
        <text class="ai-welcome__text">可以向我提问编程学习、项目开发和技术选型问题。</text>
      </view>

      <view
        v-for="(message, index) in messages"
        :id="`ai-message-${index}`"
        :key="message.id"
        class="ai-message"
        :class="{ 'ai-message--user': message.role === 'user' }"
      >
        <view class="ai-message__role">{{ message.role === 'user' ? '我' : 'AI 助手' }}</view>
        <view class="ai-message__bubble">
          <MarkdownText :content="message.content || (message.role === 'ai' && loading ? '正在思考…' : '')" />
        </view>
      </view>
    </scroll-view>

    <view v-if="errorText" class="ai-page__error">{{ errorText }}</view>
    <view class="ai-page__composer">
      <textarea
        v-model="input"
        class="ai-page__input"
        placeholder="输入编程问题…"
        :maxlength="500"
        auto-height
      />
      <button v-if="loading" class="ai-page__stop" @click="stop">停止</button>
      <button v-else class="ai-page__send" :disabled="!input.trim()" @click="send">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import MarkdownText from '@/components/MarkdownText.vue'
import { streamAiChat } from '@/realtime/aiStream'
import { requireLogin } from '@/utils/navigation'

interface AiMessage {
  id: string
  role: 'user' | 'ai'
  content: string
}

const messages = ref<AiMessage[]>([])
const input = ref('')
const loading = ref(false)
const errorText = ref('')
const scrollIntoView = ref('')
let cancelStream: (() => void) | null = null
let revealTimer: ReturnType<typeof setInterval> | null = null
let revealQueue = ''
let activeAiIndex = -1

onShow(() => requireLogin())
onUnmounted(() => stop())

function send() {
  const content = input.value.trim()
  if (!content || loading.value) return
  errorText.value = ''
  messages.value.push({ id: `user-${Date.now()}`, role: 'user', content })
  messages.value.push({ id: `ai-${Date.now()}`, role: 'ai', content: '' })
  activeAiIndex = messages.value.length - 1
  input.value = ''
  loading.value = true
  scrollToBottom()

  cancelStream = streamAiChat(content, {
    onText: (text) => {
      revealQueue += text
      startReveal()
    },
    onError: (message) => {
      flushReveal()
      errorText.value = message || 'AI 回复失败，请稍后重试'
      if (!messages.value[activeAiIndex]?.content) {
        messages.value[activeAiIndex].content = '本次回复失败，请重新发送问题。'
      }
      finish()
    },
    onComplete: () => {
      flushReveal()
      finish()
    },
  })
}

function startReveal() {
  if (revealTimer) return
  revealTimer = setInterval(() => {
    if (activeAiIndex < 0 || !revealQueue) {
      clearRevealTimer()
      return
    }
    const count = Math.min(24, Math.max(1, Math.ceil(revealQueue.length / 60)))
    messages.value[activeAiIndex].content += revealQueue.slice(0, count)
    revealQueue = revealQueue.slice(count)
    scrollToBottom()
  }, 35)
}

function flushReveal() {
  if (activeAiIndex >= 0 && revealQueue) {
    messages.value[activeAiIndex].content += revealQueue
    revealQueue = ''
  }
  clearRevealTimer()
  scrollToBottom()
}

function clearRevealTimer() {
  if (revealTimer) clearInterval(revealTimer)
  revealTimer = null
}

function finish() {
  loading.value = false
  cancelStream = null
  activeAiIndex = -1
  revealQueue = ''
}

function stop() {
  cancelStream?.()
  cancelStream = null
  flushReveal()
  finish()
}

function scrollToBottom() {
  nextTick(() => {
    if (!messages.value.length) return
    scrollIntoView.value = ''
    nextTick(() => {
      scrollIntoView.value = `ai-message-${messages.value.length - 1}`
    })
  })
}
</script>

<style scoped>
.ai-page {
  display: flex;
  height: 100vh;
  flex-direction: column;
  background: #0f172a;
}

.ai-page__messages {
  min-height: 0;
  padding: 28rpx;
  flex: 1;
}

.ai-welcome {
  display: flex;
  padding: 120rpx 36rpx;
  flex-direction: column;
  align-items: center;
}

.ai-welcome__title {
  font-size: 44rpx;
  font-weight: 800;
}

.ai-welcome__text {
  margin-top: 20rpx;
  color: #94a3b8;
  font-size: 26rpx;
  line-height: 1.7;
  text-align: center;
}

.ai-message {
  margin-bottom: 28rpx;
}

.ai-message__role {
  margin-bottom: 10rpx;
  color: #94a3b8;
  font-size: 22rpx;
}

.ai-message__bubble {
  padding: 22rpx 24rpx;
  border: 1rpx solid #334155;
  border-radius: 8rpx 24rpx 24rpx 24rpx;
  color: #e2e8f0;
  background: #1e293b;
}

.ai-message--user .ai-message__role {
  text-align: right;
}

.ai-message--user .ai-message__bubble {
  border-radius: 24rpx 8rpx 24rpx 24rpx;
  color: #0f172a;
  background: #f8fafc;
}

.ai-page__error {
  padding: 12rpx 24rpx;
  color: #fecaca;
  background: #451a1a;
  font-size: 22rpx;
}

.ai-page__composer {
  display: flex;
  padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom));
  align-items: flex-end;
  gap: 16rpx;
  border-top: 1rpx solid #334155;
  background: #1e293b;
}

.ai-page__input {
  width: auto;
  min-height: 80rpx;
  max-height: 220rpx;
  padding: 20rpx 24rpx;
  flex: 1;
  border-radius: 20rpx;
  color: #f8fafc;
  background: #0f172a;
  font-size: 26rpx;
}

.ai-page__send,
.ai-page__stop {
  width: 116rpx;
  height: 80rpx;
  padding: 0;
  flex: 0 0 auto;
  border-radius: 20rpx;
  color: #0f172a;
  background: #f8fafc;
  font-size: 25rpx;
  line-height: 80rpx;
}

.ai-page__stop {
  color: #f8fafc;
  background: #475569;
}
</style>

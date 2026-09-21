<template>
  <section class="ai-chat-page">
    <header class="ai-chat-header">
      <h1>AI 编程助手</h1>
    </header>

    <main ref="messageListRef" class="message-list">
      <article
          v-for="(message, index) in messages"
          :key="index"
          :class="['message-row', message.role === 'user' ? 'message-row--user' : 'message-row--ai']"
      >
        <template v-if="message.role === 'ai'">
          <div class="ai-message-wrap">
            <BotMessageSquare class="ai-message-icon" :size="32" :stroke-width="2" />
            <div
                class="message-bubble ai-message-bubble"
                :class="{ 'message-bubble--loading': isStreamingAiMessage(message, index) }"
            >
              <LoaderCircle
                  v-if="isStreamingAiMessage(message, index)"
                  class="loading-icon"
                  :size="22"
                  :stroke-width="2"
              />
              <div v-else class="message-text" v-html="formatMessage(message.content)"></div>
            </div>
            <time v-if="!isStreamingAiMessage(message, index)" class="message-time message-time--ai">
              {{ formatMessageTime(message.createdAt) }}
            </time>
          </div>
        </template>

        <template v-else>
          <div class="user-message-content">
            <div class="message-bubble user-message-bubble">
              <div class="message-text" v-html="formatMessage(message.content)"></div>
            </div>
            <time class="message-time message-time--user">{{ formatMessageTime(message.createdAt) }}</time>
          </div>
          <img class="user-avatar" :src="userAvatar" alt="用户头像" />
        </template>
      </article>
    </main>

    <div class="input-area">
      <div class="input-shell">
        <input
            v-model="inputText"
            class="message-input"
            type="text"
            placeholder="输入你的问题..."
            :disabled="loading"
            @keyup.enter="sendMessage"
        />
        <button
            class="send-button"
            type="button"
            aria-label="发送消息"
            :disabled="loading || !inputText.trim()"
            @click="sendMessage"
        >
          <LoaderCircle v-if="loading" class="loading-icon" :size="21" :stroke-width="2" />
          <Send v-else :size="21" :stroke-width="2.2" />
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import {computed, ref, onMounted, nextTick} from 'vue';
import {useRoute} from 'vue-router';
import DOMPurify from 'dompurify';
import {marked, Renderer} from 'marked';
import {BotMessageSquare, LoaderCircle, Send} from 'lucide-vue-next';
import {getCurrentUser} from '../services/user';
import type {UserType} from '../models/user';

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
  createdAt: number;
}

const user = ref<UserType | null>(null);
const messages = ref<ChatMessage[]>([]);
const inputText = ref('');
const loading = ref(false);
const messageListRef = ref<HTMLElement>();
const route = useRoute();
const userAvatar = computed(() => user.value?.avatarUrl || 'https://api.dicebear.com/8.x/avataaars/svg?seed=Aneka&backgroundColor=a78bfa');

marked.setOptions({
  breaks: true,
  gfm: true,
});

const markdownRenderer = new Renderer();

const escapeHtml = (text: string) => {
  return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
};

const isCommandLikeCode = (code: string) => {
  return /^(npm|pnpm|yarn|git|mvn|gradle|java|python|pip|docker|kubectl)\s+/i.test(code.trim())
      || /[=;{}\[\]|$<>]/.test(code);
};

markdownRenderer.codespan = (code) => {
  const text = code.trim();
  const escaped = escapeHtml(text);
  if (text.length <= 32 && !isCommandLikeCode(text)) {
    return `<span class="tech-token">${escaped}</span>`;
  }
  return `<code>${escaped}</code>`;
};

marked.use({
  renderer: markdownRenderer,
});

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

const normalizeMarkdown = (text: string) => {
  const source = (text || '').replace(/\\n/g, '\n').replace(/\r\n/g, '\n');
  const orderedListMarker = String.raw`\d{1,2}[.．、](?!\d)`;
  const chineseListMarker = String.raw`[一二三四五六七八九十]+[、.．]`;
  return source
      .split(/(```[\s\S]*?```)/g)
      .map((part) => {
        if (part.startsWith('```')) {
          return part;
        }
        return part
            .replace(/\*\*(?=[^\n*]{1,48}\n\s*[-*+]\s+)/g, '')
            .replace(/(\n\s*[-*+]\s+[^\n*]{1,80})\*\*/g, '$1')
            .replace(/([^\n])\s*(#{1,6}\s+)/g, '$1\n\n$2')
            .replace(/\s*[-–—]{2,}\s*([^-\n]{2,24}[：:])\s*/g, '\n\n**$1**\n\n')
            .replace(/([:：。！？!?])\s*[-–—]\s*(?=["“\u4e00-\u9fa5A-Za-z0-9])/g, '$1\n\n- ')
            .replace(/(["”'）?？])\s*[-–—]\s*(?=["“\u4e00-\u9fa5A-Za-z0-9])/g, '$1\n- ')
            .replace(new RegExp(`([:：。！？!?])\\s*(?=${orderedListMarker}\\s*)`, 'g'), '$1\n\n')
            .replace(new RegExp(`([^\\n\\d])\\s*(${orderedListMarker})\\s*`, 'g'), '$1\n$2 ')
            .replace(new RegExp(`(^|\\n)(${orderedListMarker})\\s*`, 'g'), '$1$2 ')
            .replace(/([^\n])\s+([-*+]\s+)/g, '$1\n$2')
            .replace(new RegExp(`([^\\n])\\s*(${chineseListMarker})\\s*`, 'g'), '$1\n$2 ')
            .replace(/(^|\s)\*\*(?=\S)(?![\s\S]*\*\*)/g, '$1');
      })
      .join('');
};

// 渲染 Markdown，并清理 v-html 内容。
const formatMessage = (text: string) => {
  return DOMPurify.sanitize(marked.parse(normalizeMarkdown(text)));
};

const isStreamingAiMessage = (message: ChatMessage, index: number) => {
  return loading.value
      && message.role === 'ai'
      && index === messages.value.length - 1
      && !message.content;
};

const formatMessageTime = (createdAt: number) => {
  return new Date(createdAt).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
};
// 发送消息并接收 SSE 流式响应
// silent=true 时不显示用户消息（用于隐式触发问候）
const sendAndStream = async (message: string, silent = false) => {
  loading.value = true;
  if (!silent) {
    messages.value.push({role: 'user', content: message, createdAt: Date.now()});
  }
  messages.value.push({role: 'ai', content: '', createdAt: Date.now()});
  scrollToBottom();

  try {
    const baseUrl = import.meta.env.DEV ? 'http://localhost:8080/api' : '';
    const url = `${baseUrl}/ai/chat?message=${encodeURIComponent(message)}`;

    const response = await fetch(url, {credentials: 'include'});

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let aiMessage = '';

    while (true) {
      const {done, value} = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      const lines = chunk.split('\n');

      for (const line of lines) {
        if (line.startsWith('data:')) {
          aiMessage += line.slice(5);
          messages.value[messages.value.length - 1].content = aiMessage;
        }
      }
      scrollToBottom();
    }
  } catch (e: any) {
    messages.value[messages.value.length - 1].content = '请求失败：' + (e.message || '网络错误');
  } finally {
    loading.value = false;
  }
};

// 用户手动发送
const sendMessage = () => {
  const text = inputText.value.trim();
  if (!text || loading.value) return;
  inputText.value = '';
  sendAndStream(text);
};

// 打开组件时隐式发送问候（后端会自动注入 tags 上下文）
onMounted(async () => {
  user.value = await getCurrentUser();
  // 仅当路由显式携带 silentGreet=1 时，才触发隐式问候
  if (route.query.silentGreet === '1') {
    sendAndStream('你好，请简短介绍一下你能帮我什么', true);
  }
});
</script>

<style scoped>
.ai-chat-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: #f8fafc;
  background: #0b1120;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.ai-chat-header {
  position: relative;
  z-index: 2;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  min-height: 84px;
  padding: 22px 16px 12px;
  background: #0b1120;
}

.ai-chat-header h1 {
  margin: 0;
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
  line-height: 28px;
  letter-spacing: -0.025em;
}

.message-list {
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  padding: 28px 16px 126px;
  background: #0b1120;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.message-list::-webkit-scrollbar {
  display: none;
}

.message-row {
  display: flex;
  align-items: flex-start;
  width: 100%;
  margin-bottom: 48px;
  gap: 16px;
}

.message-row:last-child {
  margin-bottom: 0;
}

.message-row--ai {
  justify-content: flex-start;
}

.message-row--user {
  justify-content: flex-end;
}

.ai-message-wrap {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  padding-top: 8px;
  padding-left: 24px;
}

.ai-message-icon {
  position: absolute;
  top: -12px;
  left: -12px;
  z-index: 2;
  color: #3b82f6;
}

.message-bubble {
  box-sizing: border-box;
  font-size: 14px;
  line-height: 1.75;
  word-break: normal;
  overflow-wrap: break-word;
}

.ai-message-bubble {
  position: relative;
  width: 100%;
  min-height: 64px;
  padding: 20px 28px 30px;
  border: 1px solid rgba(51, 65, 85, 0.5);
  border-radius: 24px 24px 24px 0;
  color: #f8fafc;
  background: #1e293b;
  box-shadow: 0 10px 25px rgba(2, 6, 23, 0.3);
}

.user-message-content {
  display: flex;
  flex: 0 1 auto;
  min-width: 0;
  max-width: calc(100% - 60px);
  flex-direction: column;
  align-items: flex-end;
}

.user-message-bubble {
  max-width: 100%;
  padding: 14px 24px;
  border-radius: 24px 24px 0 24px;
  color: #030712;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(2, 6, 23, 0.24);
}

.user-avatar {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  margin-top: 4px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  object-fit: cover;
  background: #334155;
  box-shadow: 0 8px 18px rgba(2, 6, 23, 0.3);
}

.message-bubble--loading {
  display: flex;
  min-width: 72px;
  min-height: 64px;
  align-items: center;
  justify-content: center;
  padding-bottom: 20px;
}

.loading-icon {
  animation: ai-chat-spin 1s linear infinite;
}

.message-time {
  color: #94a3b8;
  font-family: Consolas, Monaco, monospace;
  font-size: 11px;
  font-weight: 400;
  line-height: 16px;
}

.message-time--ai {
  align-self: flex-start;
  margin-top: 8px;
  padding-left: 4px;
}

.message-time--user {
  align-self: flex-end;
  margin-top: 8px;
  padding-right: 4px;
}

.message-text {
  white-space: normal;
  font-weight: 400;
}

.ai-message-bubble :deep(code) {
  display: inline;
  padding: 1px 3px;
  color: #9fd0ff;
  background: rgba(29, 144, 245, 0.1);
  border-radius: 3px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  white-space: normal;
}

.message-text :deep(.tech-token) {
  color: #9fd0ff;
  font-weight: 500;
  font-family: inherit;
  white-space: normal;
}

.message-text :deep(p) {
  margin: 0 0 10px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 8px 0 12px;
  padding-left: 22px;
  list-style-position: outside;
}

.message-text :deep(ul) {
  list-style-type: disc;
}

.message-text :deep(ol) {
  list-style-type: decimal;
}

.message-text :deep(li) {
  margin: 5px 0;
  padding-left: 2px;
}

.message-text :deep(li::marker) {
  color: #7cc0ff;
  font-weight: 600;
}

.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4) {
  margin: 12px 0 8px;
  color: #f8fafc;
  font-size: 15px;
  line-height: 1.45;
  font-weight: 700;
}

.message-text :deep(h1:first-child),
.message-text :deep(h2:first-child),
.message-text :deep(h3:first-child),
.message-text :deep(h4:first-child) {
  margin-top: 0;
}

.message-text :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 10px;
  color: #cbd5e1;
  background: rgba(255, 255, 255, 0.05);
  border-left: 3px solid #3b82f6;
  border-radius: 4px;
}

.message-text :deep(pre) {
  box-sizing: border-box;
  max-width: 100%;
  margin: 10px 0;
  padding: 10px 12px;
  overflow-x: auto;
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 6px;
}

.message-text :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #cbd5e1;
  white-space: pre;
}

.message-text :deep(a) {
  color: #7cc0ff;
  text-decoration: none;
}

.message-text :deep(strong) {
  color: #f8fafc;
  font-weight: 600;
}

.message-text :deep(table) {
  display: block;
  max-width: 100%;
  margin: 10px 0;
  overflow-x: auto;
  border-collapse: collapse;
}

.message-text :deep(th),
.message-text :deep(td) {
  padding: 6px 8px;
  border: 1px solid #334155;
}

.message-text :deep(th) {
  color: #f8fafc;
  background: #0f172a;
}

.user-message-bubble .message-text :deep(p),
.user-message-bubble .message-text :deep(strong),
.user-message-bubble .message-text :deep(h1),
.user-message-bubble .message-text :deep(h2),
.user-message-bubble .message-text :deep(h3),
.user-message-bubble .message-text :deep(h4) {
  color: #030712;
}

.input-area {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 5;
  padding: 28px 16px 22px;
  background: linear-gradient(to top, #0b1120 65%, rgba(11, 17, 32, 0));
}

.input-shell {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 18px 40px rgba(2, 6, 23, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.message-input {
  box-sizing: border-box;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  height: 48px;
  padding: 0 24px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 999px;
  outline: none;
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.65);
  font: inherit;
  font-size: 14px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.message-input::placeholder {
  color: #94a3b8;
}

.message-input:focus {
  border-color: #ffffff;
  box-shadow: 0 0 0 1px #ffffff;
}

.message-input:disabled {
  opacity: 0.72;
}

.send-button {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  color: #ffffff;
  background: #3b82f6;
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.3);
  transition: opacity 0.18s ease, transform 0.15s ease, background 0.18s ease;
}

.send-button:not(:disabled):active {
  transform: scale(0.93);
  background: #2563eb;
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

@keyframes ai-chat-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 359px) {
  .message-list {
    padding-right: 12px;
    padding-left: 12px;
  }

  .ai-message-bubble {
    padding-right: 20px;
    padding-left: 22px;
  }

  .input-area {
    padding-right: 12px;
    padding-left: 12px;
  }
}
</style>





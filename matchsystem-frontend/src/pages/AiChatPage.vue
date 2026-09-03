<template>
  <div class="chat-container mall-chat">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-for="(msg, index) in messages" :key="index"
           :class="['message', msg.role === 'user' ? 'message-user' : 'message-ai']">
        <div class="message-avatar">
          <van-image v-if="msg.role === 'user'" round width="32" height="32"
                     :src="user?.avatarUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'" />
          <van-icon v-else name="chat" size="32" color="#1d90f5" />
        </div>
        <div class="message-bubble" :class="{ 'message-bubble-loading': isStreamingAiMessage(msg, index) }">
          <van-loading v-if="isStreamingAiMessage(msg, index)" size="16" />
          <div v-else class="message-text" v-html="formatMessage(msg.content)"></div>
        </div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-bar">
      <van-field v-model="inputText" placeholder="输入你的问题..." @keyup.enter="sendMessage"
                 :disabled="loading" />
      <van-button type="primary" size="small" @click="sendMessage" :disabled="loading || !inputText.trim()">
        发送
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, nextTick} from 'vue';
import {useRoute} from 'vue-router';
import DOMPurify from 'dompurify';
import {marked, Renderer} from 'marked';
import {getCurrentUser} from '../services/user';
import type {UserType} from '../models/user';

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
}

const user = ref<UserType | null>(null);
const messages = ref<ChatMessage[]>([]);
const inputText = ref('');
const loading = ref(false);
const messageListRef = ref<HTMLElement>();
const route = useRoute();

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

// 发送消息并接收 SSE 流式响应
// silent=true 时不显示用户消息（用于隐式触发问候）
const sendAndStream = async (message: string, silent = false) => {
  loading.value = true;
  if (!silent) {
    messages.value.push({role: 'user', content: message});
  }
  messages.value.push({role: 'ai', content: ''});
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
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--bg-page);
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 12px 18px;
  background:
    radial-gradient(900px 400px at 85% -10%, rgba(29, 144, 245, 0.12), transparent 60%),
    var(--bg-page);
}

.message {
  display: flex;
  align-items: flex-start;
  margin-bottom: 14px;
  gap: 10px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-bubble {
  max-width: 75%;
  box-sizing: border-box;
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: normal;
  overflow-wrap: break-word;
}

.message-text {
  white-space: normal;
  font-weight: 400;
}

.message-user .message-bubble {
  max-width: 68%;
  background: var(--color-primary);
  color: white;
}

.message-ai .message-bubble {
  max-width: min(calc(100% - 46px), 640px);
  background: var(--bg-elevated);
  color: var(--text-main);
  border: 1px solid var(--border-weak);
}

.message-bubble-loading {
  min-width: 52px;
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-ai .message-bubble :deep(code) {
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
  color: var(--text-main);
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
  color: var(--text-light-1);
  background: rgba(255, 255, 255, 0.05);
  border-left: 3px solid var(--color-primary);
  border-radius: 4px;
}

.message-text :deep(pre) {
  box-sizing: border-box;
  max-width: 100%;
  margin: 10px 0;
  padding: 10px 12px;
  overflow-x: auto;
  background: var(--bg-panel);
  border: 1px solid var(--border-weak);
  border-radius: 6px;
}

.message-text :deep(pre code) {
  padding: 0;
  background: transparent;
  color: var(--text-light-1);
  white-space: pre;
}

.message-text :deep(a) {
  color: #7cc0ff;
  text-decoration: none;
}

.message-text :deep(strong) {
  color: var(--text-main);
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
  border: 1px solid var(--border-weak);
}

.message-text :deep(th) {
  color: var(--text-main);
  background: var(--bg-panel);
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: var(--bg-panel);
  border-top: 1px solid var(--border-weak);
  gap: 8px;
}

.input-bar .van-field {
  flex: 1;
}

.input-bar :deep(.van-field__control) {
  color: var(--text-main);
}

.input-bar :deep(.van-field__control::placeholder) {
  color: var(--text-placeholder);
}

.message-avatar {
  flex-shrink: 0;
}
</style>

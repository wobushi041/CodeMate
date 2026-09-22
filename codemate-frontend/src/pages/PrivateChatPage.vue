<template>
  <SubPageLayout ref="layoutRef" :title="targetUsername || '单人聊天室'">
    <div class="chat-content">
      <section class="team-info-card">
        <div>
          <h2>{{ targetUsername || '私聊伙伴' }}</h2>
          <p class="connection-status">
            <span class="connection-dot" :class="{ 'connection-dot--online': targetOnline }" />
            {{ statusText }}
          </p>
        </div>
        <button class="refresh-button" type="button" :disabled="loadingHistory" @click="loadHistory">
          <RefreshCw :class="{ 'is-spinning': loadingHistory }" :size="16" :stroke-width="1.9" />
          <span>{{ loadingHistory ? '正在刷新...' : '刷新记录' }}</span>
        </button>
      </section>

      <div v-if="loadingHistory && !messages.length" class="chat-state">
        <LoaderCircle class="is-spinning" :size="26" />
        <span>加载中...</span>
      </div>
      <div v-else-if="!messages.length" class="chat-state">
        <p>暂无聊天记录，发送一条消息开启畅聊吧</p>
      </div>

      <div v-else class="message-list">
        <article
          v-for="message in messages"
          :key="messageKey(message)"
          class="message-row"
          :class="{ 'message-row--self': isSelfMessage(message) }"
        >
          <img
            v-if="!isSelfMessage(message)"
            class="message-avatar"
            :src="avatarSource(message)"
            :alt="senderName(message)"
          />
          <div class="message-content" :class="{ 'message-content--self': isSelfMessage(message) }">
            <div v-if="!isSelfMessage(message)" class="message-meta">
              <span>{{ senderName(message) }}</span>
              <time>{{ formatMessageTime(message.createTime) }}</time>
            </div>
            <div class="message-bubble" :class="{ 'message-bubble--self': isSelfMessage(message) }">
              {{ message.content || message.message }}
            </div>
            <time v-if="isSelfMessage(message)" class="message-time">{{ formatMessageTime(message.createTime) }}</time>
          </div>
          <img
            v-if="isSelfMessage(message)"
            class="message-avatar message-avatar--self"
            :src="avatarSource(message)"
            :alt="senderName(message)"
          />
        </article>
      </div>
    </div>

    <template #bottom>
      <div class="composer">
        <input
          v-model="inputContent"
          maxlength="2048"
          type="text"
          placeholder="输入私聊消息..."
          @keydown.enter.prevent="sendMessage"
        />
        <button class="send-button" type="button" :disabled="!canSend" @click="sendMessage">
          <Send :size="20" :stroke-width="1.9" />
        </button>
      </div>
    </template>
  </SubPageLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Toast } from 'vant';
import { LoaderCircle, RefreshCw, Send } from 'lucide-vue-next';
import SubPageLayout from '../components/SubPageLayout.vue';
import myAxios from '../plugins/myAxios';
import { getCurrentUser } from '../services/user';
import { UserType } from '../models/user';
import { markUserContacted } from '../services/privateChat';

type ChatMessage = {
  type?: string;
  sessionId?: number;
  messageId?: number;
  clientMessageId?: string;
  fromUserId?: number;
  fromUsername?: string;
  toUserId?: number;
  content?: string;
  message?: string;
  createTime?: string;
};

type ChatMessagePage = {
  records?: ChatMessage[];
};

const route = useRoute();
const router = useRouter();

const sessionId = ref<number>(Number(route.query.sessionId) || 0);
const targetUserId = ref<number>(Number(route.query.targetUserId) || 0);
const targetUsername = ref<string>(String(route.query.targetUsername || ''));
const targetAvatarUrl = ref<string>(String(route.query.targetAvatarUrl || ''));
const targetOnline = ref<boolean>(route.query.isOnline === 'true' || route.query.isOnline === '1');

const currentUser = ref<UserType | null>(null);
const messages = ref<ChatMessage[]>([]);
const inputContent = ref('');
const loadingHistory = ref(false);
const connected = ref(false);
const joined = ref(false);
const layoutRef = ref<InstanceType<typeof SubPageLayout> | null>(null);

let ws: WebSocket | null = null;

const statusText = computed(() => {
  if (joined.value) {
    return targetOnline.value ? '已进入会话 (对方在线)' : '已进入会话 (对方离线，可留言)';
  }
  if (connected.value) {
    return '正在同步会话...';
  }
  return '连接中...';
});

const canSend = computed(() => joined.value && inputContent.value.trim().length > 0);

onMounted(async () => {
  if (!sessionId.value && !targetUserId.value) {
    Toast.fail('缺少私聊会话参数');
    router.back();
    return;
  }

  currentUser.value = await getCurrentUser();
  markUserContacted(targetUserId.value);

  // 若没有 sessionId，先通过 targetUserId 初始化或获取会话
  if (!sessionId.value && targetUserId.value) {
    try {
      const res: any = await myAxios.post('/chat/private/start', {
        targetUserId: targetUserId.value,
      });
      if (res?.code === 0 && res?.data) {
        sessionId.value = Number(res.data.sessionId);
        targetOnline.value = !!res.data.isTargetOnline;
        if (!targetUsername.value && res.data.targetUser?.userName) {
          targetUsername.value = res.data.targetUser.userName;
        }
        if (!targetAvatarUrl.value && res.data.targetUser?.avatarUrl) {
          targetAvatarUrl.value = res.data.targetUser.avatarUrl;
        }
      } else {
        Toast.fail(res?.description || '获取私聊会话失败');
        router.back();
        return;
      }
    } catch (e) {
      Toast.fail('初始化私聊会话失败');
      router.back();
      return;
    }
  }

  await loadHistory();
  connectWebSocket();
});

onBeforeUnmount(() => {
  if (ws) {
    ws.close();
    ws = null;
  }
});

const loadHistory = async () => {
  if (!sessionId.value) {
    return;
  }
  loadingHistory.value = true;
  try {
    const res: any = await myAxios.get('/chat/private/messages', {
      params: {
        sessionId: sessionId.value,
        pageNum: 1,
        pageSize: 30,
      },
    });
    if (res?.code === 0) {
      const page: ChatMessagePage = res.data || {};
      messages.value = [...(page.records || [])].reverse();
      if (messages.value.length) {
        markUserContacted(targetUserId.value);
      }
      await scrollToBottom();
    } else {
      Toast.fail(res?.description || '加载私聊记录失败');
    }
  } catch (e) {
    Toast.fail('加载私聊记录失败');
  } finally {
    loadingHistory.value = false;
  }
};

const getWebSocketUrl = () => {
  const configuredUrl = import.meta.env.VITE_WS_BASE_URL;
  if (configuredUrl) {
    return configuredUrl;
  }
  if (import.meta.env.DEV) {
    return 'ws://localhost:8091/ws/chat';
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.hostname}:8091/ws/chat`;
};

const connectWebSocket = () => {
  ws = new WebSocket(getWebSocketUrl());

  ws.onopen = () => {
    connected.value = true;
    // 发送 PRIVATE_JOIN 绑定私聊会话
    ws?.send(
      JSON.stringify({
        type: 'PRIVATE_JOIN',
        sessionId: sessionId.value,
      })
    );
  };

  ws.onmessage = async (event) => {
    const data = parseMessage(event.data);
    if (!data) {
      return;
    }
    if (data.type === 'PRIVATE_JOINED') {
      joined.value = true;
      return;
    }
    if (data.type === 'PRIVATE_CHAT') {
      appendMessage(data);
      await scrollToBottom();
      return;
    }
    if (data.type === 'ERROR') {
      Toast.fail(data.message || '私聊错误');
    }
  };

  ws.onerror = () => {
    Toast.fail('WebSocket 连接异常');
  };

  ws.onclose = (event) => {
    connected.value = false;
    joined.value = false;
    if (!event.wasClean) {
      Toast.fail(`WebSocket 连接失败(${event.code})`);
    }
  };
};

const sendMessage = () => {
  const content = inputContent.value.trim();
  if (!content) {
    return;
  }
  if (!ws || ws.readyState !== WebSocket.OPEN || !joined.value) {
    Toast.fail('聊天室还未连接');
    return;
  }
  ws.send(
    JSON.stringify({
      type: 'PRIVATE_CHAT',
      sessionId: sessionId.value,
      clientMessageId: createClientMessageId(),
      content,
    })
  );
  inputContent.value = '';
};

const parseMessage = (data: string): ChatMessage | null => {
  try {
    return JSON.parse(data);
  } catch (e) {
    Toast.fail('消息格式错误');
    return null;
  }
};

const appendMessage = (message: ChatMessage) => {
  if (messages.value.some((item) => messageKey(item) === messageKey(message))) {
    return;
  }
  messages.value.push(message);
};

const scrollToBottom = async () => {
  await nextTick();
  layoutRef.value?.scrollToBottom();
};

const messageKey = (message: ChatMessage) => {
  return (
    message.messageId ||
    message.clientMessageId ||
    `${message.fromUserId}-${message.createTime}-${message.content}`
  );
};

const isSelfMessage = (message: ChatMessage) => {
  return Number(message.fromUserId) === Number(currentUser.value?.id);
};

const senderName = (message: ChatMessage) => {
  if (isSelfMessage(message)) {
    return '我';
  }
  return message.fromUsername || targetUsername.value || `用户 ${message.fromUserId || ''}`;
};

const avatarText = (message: ChatMessage) => {
  const name = senderName(message);
  return name.slice(0, 1).toUpperCase();
};

const avatarSource = (message: ChatMessage) => {
  if (isSelfMessage(message)) {
    if (currentUser.value?.avatarUrl) {
      return currentUser.value.avatarUrl;
    }
  } else {
    if (targetAvatarUrl.value) {
      return targetAvatarUrl.value;
    }
  }
  const seed = encodeURIComponent(senderName(message) || avatarText(message));
  return `https://api.dicebear.com/8.x/initials/svg?seed=${seed}&backgroundColor=475569&fontFamily=Arial`;
};

const formatMessageTime = (value?: string) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const createClientMessageId = () => {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};
</script>

<style scoped>
.chat-content {
  box-sizing: border-box;
  width: 100%;
  padding: 0 16px 16px;
  overflow-x: hidden;
}

.team-info-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 10px 0 28px;
  padding: 20px;
  border: 1px solid rgba(100, 116, 139, 0.35);
  border-radius: 16px;
  background: #1e293b;
  box-shadow: 0 16px 30px rgba(2, 6, 23, 0.2);
}

.team-info-card h2 {
  margin: 0;
  overflow: hidden;
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
  line-height: 26px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-info-card p {
  margin: 4px 0 0;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 18px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 7px;
}

.connection-dot {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #64748b;
  box-shadow: 0 0 0 3px rgba(100, 116, 139, 0.12);
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.connection-dot--online {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.14);
  animation: connection-pulse 2s ease-in-out infinite;
}

.refresh-button {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  color: #f8fafc;
  background: #334155;
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 4px 8px rgba(2, 6, 23, 0.16);
}

.refresh-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.is-spinning {
  animation: spin 1s linear infinite;
}

.message-list {
  display: grid;
  gap: 28px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 14px;
}

.message-row--self {
  justify-content: flex-end;
  align-items: flex-start;
}

.message-avatar {
  display: grid;
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  place-items: center;
  margin-bottom: 3px;
  border: 2px solid #334155;
  border-radius: 50%;
  color: #f8fafc;
  background: #475569;
  font-size: 15px;
  font-weight: 700;
  object-fit: cover;
}

.message-avatar--self {
  margin-top: 4px;
  margin-bottom: 0;
  border-color: #fff;
  color: #030712;
  background: #fff;
}

.message-content {
  display: flex;
  min-width: 0;
  max-width: calc(100% - 58px);
  flex-direction: column;
  align-items: flex-start;
}

.message-content--self {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 8px;
  padding-left: 4px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 16px;
}

.message-meta span {
  color: #cbd5e1;
  font-weight: 600;
}

.message-meta time,
.message-time {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.message-bubble {
  max-width: 100%;
  box-sizing: border-box;
  padding: 14px 24px;
  border: 1px solid rgba(100, 116, 139, 0.45);
  border-radius: 16px 16px 16px 3px;
  color: #f8fafc;
  background: #1e293b;
  font-size: 14px;
  line-height: 22px;
  word-break: break-word;
  box-shadow: 0 10px 18px rgba(2, 6, 23, 0.16);
}

.message-bubble--self {
  border-color: #fff;
  border-radius: 16px 16px 3px 16px;
  color: #030712;
  background: #fff;
}

.message-time {
  margin-top: 8px;
  padding-right: 4px;
  color: #94a3b8;
}

.chat-state {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 13px;
}

.chat-state p {
  margin: 0;
}

.composer {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 18px 35px rgba(2, 6, 23, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.composer input {
  min-width: 0;
  flex: 1;
  height: 48px;
  padding: 0 20px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 999px;
  outline: 0;
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.6);
  font: inherit;
  font-size: 14px;
}

.composer input::placeholder {
  color: #94a3b8;
}

.composer input:focus {
  border-color: #fff;
  box-shadow: 0 0 0 1px #fff;
}

.send-button {
  display: grid;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: #3b82f6;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.send-button:active,
.refresh-button:active {
  transform: scale(0.95);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes connection-pulse {
  0%,
  100% {
    opacity: 1;
    box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.14);
  }
  50% {
    opacity: 0.62;
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.05);
  }
}

@media (max-width: 360px) {
  .team-info-card {
    padding: 16px;
  }
  .refresh-button {
    padding: 0 12px;
  }
  .message-bubble {
    padding: 12px 18px;
  }
  .chat-content {
    padding-right: 12px;
    padding-left: 12px;
  }
}
</style>

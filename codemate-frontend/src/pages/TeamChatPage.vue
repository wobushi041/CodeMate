<template>
  <SubPageLayout ref="layoutRef" title="队伍聊天室">
    <template #headerCenter>
      <div class="chat-header-center">
        <h1 :title="teamName">{{ teamName }}</h1>
        <p class="connection-status" :title="statusText">
          <span class="connection-dot" :class="{ 'connection-dot--online': connected }" />
          <span>{{ statusText }}</span>
        </p>
      </div>
    </template>
    <template #headerRight>
      <button class="refresh-button chat-header-refresh" type="button" :aria-label="loadingHistory ? '正在刷新记录' : '刷新记录'" :title="loadingHistory ? '正在刷新记录' : '刷新记录'" :disabled="loadingHistory" @click="loadHistory">
        <RefreshCw :class="{ 'is-spinning': loadingHistory }" :size="18" :stroke-width="1.9" />
      </button>
    </template>
    <div class="chat-content">
      <div v-if="loadingHistory && !messages.length" class="chat-state">
        <LoaderCircle class="is-spinning" :size="26" />
        <span>加载中...</span>
      </div>
      <div v-else-if="!messages.length" class="chat-state">
        <p>暂无聊天消息</p>
      </div>

      <div v-else class="message-list">
        <article
          v-for="message in messages"
          :key="messageKey(message)"
          class="message-row"
          :class="{ 'message-row--self': isSelfMessage(message) }"
        >
          <img v-if="!isSelfMessage(message)" class="message-avatar" :src="avatarSource(message)" :alt="senderName(message)" />
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
          <img v-if="isSelfMessage(message)" class="message-avatar message-avatar--self" :src="avatarSource(message)" :alt="senderName(message)" />
        </article>
      </div>
    </div>

    <template #bottom>
      <div class="composer">
        <input
          v-model="inputContent"
          maxlength="2048"
          type="text"
          placeholder="输入消息..."
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Toast } from "vant";
import { LoaderCircle, RefreshCw, Send } from "lucide-vue-next";
import SubPageLayout from "../components/SubPageLayout.vue";
import myAxios from "../plugins/myAxios";
import { getCurrentUser } from "../services/user";
import { UserType } from "../models/user";

type ChatMessage = {
  type?: string;
  teamId?: number;
  messageId?: number;
  clientMessageId?: string;
  fromUserId?: number;
  fromUsername?: string;
  content?: string;
  message?: string;
  createTime?: string;
};

type ChatMessagePage = {
  records?: ChatMessage[];
};

const route = useRoute();
const router = useRouter();

const teamId = Number(route.query.teamId);
const teamName = computed(() => String(route.query.teamName || `队伍 ${teamId}`));
const currentUser = ref<UserType | null>(null);
const messages = ref<ChatMessage[]>([]);
const inputContent = ref("");
const loadingHistory = ref(false);
const connected = ref(false);
const joined = ref(false);
const layoutRef = ref<InstanceType<typeof SubPageLayout> | null>(null);

let ws: WebSocket | null = null;

const statusText = computed(() => {
  if (joined.value) {
    return "已进入聊天室";
  }
  if (connected.value) {
    return "正在加入聊天室";
  }
  return "连接中";
});

const canSend = computed(() => joined.value && inputContent.value.trim().length > 0);

onMounted(async () => {
  if (!teamId || Number.isNaN(teamId)) {
    Toast.fail("缺少 teamId");
    router.back();
    return;
  }
  currentUser.value = await getCurrentUser();
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
  loadingHistory.value = true;
  try {
    const res: any = await myAxios.get(`/chat/teams/${teamId}/messages`, {
      params: {
        pageNum: 1,
        pageSize: 20,
      },
    });
    if (res?.code === 0) {
      const page: ChatMessagePage = res.data || {};
      messages.value = [...(page.records || [])].reverse();
      await scrollToBottom();
    } else {
      Toast.fail(res?.description || "加载聊天记录失败");
    }
  } catch (e) {
    Toast.fail("加载聊天记录失败");
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
    return "ws://localhost:8091/ws/chat";
  }
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.hostname}:8091/ws/chat`;
};

const connectWebSocket = () => {
  ws = new WebSocket(getWebSocketUrl());

  ws.onopen = () => {
    connected.value = true;
    ws?.send(JSON.stringify({
      type: "JOIN",
      teamId,
    }));
  };

  ws.onmessage = async (event) => {
    const data = parseMessage(event.data);
    if (!data) {
      return;
    }
    if (data.type === "JOINED") {
      joined.value = true;
      return;
    }
    if (data.type === "CHAT") {
      appendMessage(data);
      await scrollToBottom();
      return;
    }
    if (data.type === "ERROR") {
      Toast.fail(data.message || "聊天室错误");
    }
  };

  ws.onerror = () => {
    Toast.fail("WebSocket 连接异常");
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
    Toast.fail("聊天室还未连接");
    return;
  }
  ws.send(JSON.stringify({
    type: "CHAT",
    clientMessageId: createClientMessageId(),
    content,
  }));
  inputContent.value = "";
};

const parseMessage = (data: string): ChatMessage | null => {
  try {
    return JSON.parse(data);
  } catch (e) {
    Toast.fail("消息格式错误");
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
  return message.messageId || message.clientMessageId || `${message.fromUserId}-${message.createTime}-${message.content}`;
};

const isSelfMessage = (message: ChatMessage) => {
  return message.fromUserId === currentUser.value?.id;
};

const senderName = (message: ChatMessage) => {
  if (isSelfMessage(message)) {
    return "我";
  }
  return message.fromUsername || `用户 ${message.fromUserId || ""}`;
};

const avatarText = (message: ChatMessage) => {
  const name = senderName(message);
  return name.slice(0, 1).toUpperCase();
};
const avatarSource = (message: ChatMessage) => {
  if (isSelfMessage(message) && currentUser.value?.avatarUrl) {
    return currentUser.value.avatarUrl;
  }
  const seed = encodeURIComponent(senderName(message) || avatarText(message));
  return `https://api.dicebear.com/8.x/initials/svg?seed=${seed}&backgroundColor=475569&fontFamily=Arial`;
};

const formatMessageTime = (value?: string) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (number: number) => String(number).padStart(2, '0');
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
  .message-bubble {
    padding: 12px 18px;
  }
  .chat-content {
    padding-right: 12px;
    padding-left: 12px;
  }
}
</style>

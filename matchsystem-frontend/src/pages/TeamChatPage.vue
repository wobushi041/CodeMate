<template>
  <div class="team-chat-page">
    <div class="chat-header">
      <div>
        <div class="room-name">{{ teamName }}</div>
        <div class="room-status" :class="{ online: joined }">{{ statusText }}</div>
      </div>
      <van-button size="small" plain type="primary" @click="loadHistory">刷新记录</van-button>
    </div>

    <div ref="messageListRef" class="message-list">
      <div v-if="loadingHistory" class="loading-wrap">
        <van-loading size="24px">加载中...</van-loading>
      </div>
      <van-empty v-else-if="messages.length === 0" description="暂无聊天消息" />
      <div
          v-for="message in messages"
          :key="messageKey(message)"
          class="message-row"
          :class="{ self: isSelfMessage(message) }"
      >
        <div class="avatar">
          {{ avatarText(message) }}
        </div>
        <div class="message-main">
          <div class="message-meta">
            <span>{{ senderName(message) }}</span>
            <span>{{ message.createTime }}</span>
          </div>
          <div class="message-bubble">
            {{ message.content }}
          </div>
        </div>
      </div>
    </div>

    <div class="composer">
      <van-field
          v-model="inputContent"
          class="composer-input"
          type="textarea"
          rows="2"
          autosize
          maxlength="2048"
          show-word-limit
          placeholder="输入消息"
      />
      <van-button
          class="send-button"
          type="primary"
          :disabled="!canSend"
          @click="sendMessage"
      >
        发送
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {Toast} from "vant";
import myAxios from "../plugins/myAxios";
import {getCurrentUser} from "../services/user";
import {UserType} from "../models/user";

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
const messageListRef = ref<HTMLDivElement | null>(null);

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
  const el = messageListRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
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

const createClientMessageId = () => {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};
</script>

<style scoped>
.team-chat-page {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  background:
    radial-gradient(900px 400px at 85% -10%, rgba(29, 144, 245, 0.12), transparent 60%),
    var(--bg-page);
  display: flex;
  flex-direction: column;
}

.chat-header {
  height: 58px;
  padding: 8px 14px;
  background: var(--bg-panel);
  border-bottom: 1px solid var(--border-weak);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.room-name {
  max-width: 220px;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-status {
  color: var(--text-light);
  font-size: 12px;
  line-height: 18px;
}

.room-status.online {
  color: var(--color-wechat);
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 14px 12px;
  overflow-y: auto;
}

.loading-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.message-row.self {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.message-row.self .avatar {
  background: var(--color-wechat);
}

.message-main {
  max-width: min(72vw, 520px);
}

.message-meta {
  margin-bottom: 4px;
  color: var(--text-light);
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 16px;
}

.message-row.self .message-meta {
  justify-content: flex-end;
}

.message-bubble {
  padding: 9px 11px;
  border-radius: 6px;
  background: var(--bg-elevated);
  color: var(--text-main);
  font-size: 15px;
  line-height: 22px;
  word-break: break-word;
  border: 1px solid var(--border-weak);
}

.message-row.self .message-bubble {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.composer {
  padding: 10px 12px;
  background: var(--bg-panel);
  border-top: 1px solid var(--border-weak);
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.composer-input {
  flex: 1;
  border-radius: 6px;
  background: var(--bg-elevated);
}

.composer-input :deep(.van-field__control) {
  color: var(--text-main);
}

.composer-input :deep(.van-field__control::placeholder) {
  color: var(--text-placeholder);
}

.send-button {
  width: 72px;
  height: 44px;
  flex: 0 0 72px;
}
</style>

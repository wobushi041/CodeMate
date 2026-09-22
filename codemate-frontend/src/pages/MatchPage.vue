<template>
  <section class="match-page">
    <header class="match-header">
      <div>
        <h1>心动匹配</h1>
        <span>根据你的技术方向，为你精选合适拍档</span>
      </div>
      <button type="button" aria-label="刷新匹配" :disabled="loading" @click="loadMatches">
        <RefreshCw :class="{ spinning: loading }" :size="21" :stroke-width="1.9" />
      </button>
    </header>

    <main class="match-content">
      <h3 v-if="loading || currentPartner" class="match-recommendation-title">
        <Sparkles :size="16" :stroke-width="2" />
        今日推荐
      </h3>

      <div v-if="loading" class="match-card match-card--loading" aria-label="正在匹配">
        <span class="loading-pill" />
        <span class="loading-avatar" />
        <span class="loading-line loading-line--name" />
        <span class="loading-line loading-line--profile" />
        <div class="loading-tags"><span /><span /><span /></div>
      </div>

      <article v-else-if="currentPartner" class="match-card">
        <div class="match-card__topline">
          <span class="match-card__score">{{ recommendationScore }}%</span>
        </div>

        <div class="match-avatar-wrap">
          <img
              class="match-avatar"
              :src="currentPartner.avatarUrl || fallbackAvatar"
              :alt="currentPartner.username"
              @error="onAvatarError"
          />
          <span class="match-online" />
        </div>

        <div class="match-identity">
          <h2>{{ formatUserName(currentPartner) }}</h2>
        </div>

        <div class="match-tags">
          <span v-for="tag in visibleTags" :key="tag">{{ tag }}</span>
          <span v-if="currentPartner.tags.length > visibleTags.length">+{{ currentPartner.tags.length - visibleTags.length }}</span>
        </div>

        <p class="match-profile">
          {{ currentPartner.profile || '这位开发者暂时没有填写个人简介，可以从共同技术栈开始认识。' }}
        </p>

        <section class="match-reasons">
          <div class="match-section-title">
            <Target :size="18" :stroke-width="1.9" />
            <h3>推荐理由</h3>
          </div>
          <ul>
            <li v-for="reason in matchReasons" :key="reason">
              <Check :size="15" :stroke-width="2.2" />
              <span>{{ reason }}</span>
            </li>
          </ul>
        </section>

        <div class="match-progress" aria-label="匹配进度">
          <span
              v-for="(_, index) in userList"
              :key="index"
              :class="{ active: index === currentIndex }"
          />
        </div>
      </article>

      <div v-else class="match-empty">
        <UsersRound :size="58" :stroke-width="1.4" />
        <h2>暂时没有新的匹配</h2>
        <p>稍后再来看看，系统会持续为你寻找合适拍档。</p>
        <button type="button" @click="loadMatches"><RefreshCw :size="18" />重新匹配</button>
      </div>
    </main>

    <footer v-if="!loading && currentPartner" class="match-actions">
      <button class="match-action match-action--skip" type="button" @click="nextPartner('已跳过当前用户')">
        <X :size="22" :stroke-width="2" />
        <span>跳过</span>
      </button>
      <button
        class="match-action match-action--contact"
        type="button"
        :disabled="contacting"
        @click="contactPartner"
      >
        <LoaderCircle v-if="contacting" class="is-spinning" :size="22" />
        <MessageCircle v-else :size="22" :stroke-width="2" />
        <span>{{ contacting ? '连接中...' : '联系 TA' }}</span>
      </button>
      <button class="match-action match-action--next" type="button" @click="nextPartner()">
        <ArrowRight :size="22" :stroke-width="2" />
        <span>下一个</span>
      </button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {
  ArrowRight,
  Check,
  LoaderCircle,
  MapPin,
  MessageCircle,
  RefreshCw,
  Sparkles,
  Target,
  UsersRound,
  X,
} from 'lucide-vue-next';
import {Toast} from 'vant';
import myAxios from '../plugins/myAxios';
import type {UserType} from '../models/user';

type Partner = Omit<UserType, 'tags'> & {tags: string[]};

const router = useRouter();
const userList = ref<Partner[]>([]);
const currentIndex = ref(0);
const loading = ref(true);
const fallbackAvatar = 'https://api.dicebear.com/8.x/initials/svg?seed=Code&backgroundColor=334155&fontFamily=Arial';

const normalizeTags = (rawTags: unknown): string[] => {
  if (!rawTags) return [];
  if (Array.isArray(rawTags)) return rawTags.map(String);
  try {
    const parsed = JSON.parse(String(rawTags));
    return Array.isArray(parsed) ? parsed.map(String) : [String(parsed)];
  } catch {
    return String(rawTags).split(/[,，]/).map(tag => tag.trim()).filter(Boolean);
  }
};

const currentPartner = computed(() => userList.value[currentIndex.value] ?? null);
const visibleTags = computed(() => currentPartner.value?.tags.slice(0, 4) ?? []);
const recommendationScore = computed(() => Math.max(78, 96 - currentIndex.value * 3));
const matchReasons = computed(() => {
  const partner = currentPartner.value;
  if (!partner) return [];
  const reasons = partner.tags.slice(0, 2).map(tag => `技术方向包含 ${tag}`);
  if (partner.profile) reasons.push('个人简介完整，便于快速了解协作方向');
  reasons.push('近期活跃，可以及时开始沟通');
  return reasons.slice(0, 3);
});

const loadMatches = async () => {
  loading.value = true;
  try {
    const response = await myAxios.get('/user/match', {params: {num: 20}});
    userList.value = Array.isArray(response?.data)
      ? response.data.map((user: UserType) => ({...user, tags: normalizeTags(user.tags)}))
      : [];
    currentIndex.value = 0;
  } catch (error) {
    console.error('/user/match error', error);
    userList.value = [];
    Toast.fail('匹配失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const formatUserName = (partner: Partner) => {
  const name = partner.username || partner.userAccount || '匿名开发者';
  return partner.planetCode ? `${name} · ${partner.planetCode}` : name;
};

const onAvatarError = (event: Event) => {
  const image = event.target as HTMLImageElement;
  if (image.src !== fallbackAvatar) image.src = fallbackAvatar;
};

const nextPartner = (message?: string) => {
  if (message) Toast(message);
  if (userList.value.length <= 1) {
    Toast('当前只有这一位推荐用户');
    return;
  }
  currentIndex.value = (currentIndex.value + 1) % userList.value.length;
};

const contacting = ref(false);

const contactPartner = async () => {
  const partner = currentPartner.value;
  if (!partner || !partner.id) {
    Toast.fail('未获取到当前拍档信息');
    return;
  }
  if (contacting.value) {
    return;
  }
  contacting.value = true;
  try {
    const res: any = await myAxios.post('/chat/private/start', {
      targetUserId: Number(partner.id),
    });
    if (res?.code === 0 && res?.data) {
      const session = res.data;
      const targetName = session.targetUser?.userName || partner.username || partner.userAccount || '该拍档';
      const targetAvatar = session.targetUser?.avatarUrl || partner.avatarUrl || '';
      router.push({
        path: '/chat/private',
        query: {
          sessionId: String(session.sessionId),
          targetUserId: String(partner.id),
          targetUsername: targetName,
          targetAvatarUrl: targetAvatar,
          isOnline: session.isTargetOnline ? '1' : '0',
        },
      });
    } else {
      Toast.fail(res?.description || res?.message || '发起私聊失败');
    }
  } catch (error: any) {
    console.error('/chat/private/start error', error);
    const msg = error?.response?.data?.description || error?.response?.data?.message || '请求失败，请稍后重试';
    Toast.fail(msg);
  } finally {
    contacting.value = false;
  }
};

onMounted(loadMatches);
</script>

<style scoped>
.match-page {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  min-height: 100%;
  flex-direction: column;
  padding: 30px 16px 28px;
  color: #f8fafc;
  background: #0f172a;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.match-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.match-header p {
  margin: 0 0 5px;
  color: #60a5fa;
  font-size: 10px;
  font-weight: 700;
  line-height: 14px;
  letter-spacing: 0.16em;
}

.match-header h1 {
  margin: 0;
  color: #f8fafc;
  font-size: 24px;
  font-weight: 700;
  line-height: 32px;
  letter-spacing: -0.025em;
}

.match-header span {
  display: block;
  margin-top: 4px;
  color: #cbd5e1;
  font-size: 14px;
  line-height: 20px;
}

.match-header button {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  margin-top: 8px;
  padding: 0;
  place-items: center;
  border: 1px solid #334155;
  border-radius: 50%;
  color: #cbd5e1;
  background: #1e293b;
}

.match-header button:disabled {
  opacity: 0.65;
}

.match-content {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}

.match-recommendation-title {
  display: flex;
  width: 100%;
  max-width: 430px;
  align-items: center;
  gap: 7px;
  margin: 0 auto 10px;
  color: #f8fafc;
  font-size: 16px;
  font-weight: 700;
  line-height: 22px;
}

.match-recommendation-title svg {
  color: #60a5fa;
}

.match-card {
  position: relative;
  width: 100%;
  max-width: 430px;
  box-sizing: border-box;
  padding: 18px 18px 20px;
  overflow: hidden;
  border: 1px solid rgba(96, 165, 250, 0.24);
  border-radius: 28px;
  background:
    radial-gradient(circle at 50% 4%, rgba(59, 130, 246, 0.2), transparent 32%),
    #1e293b;
  box-shadow: 0 22px 45px rgba(2, 6, 23, 0.34);
  margin-right: auto;
  margin-left: auto;
}

.match-card__topline {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 18px;
}

.match-card__score {
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
  line-height: 28px;
}

.match-card__score::after {
  display: block;
  margin-top: -2px;
  color: #94a3b8;
  content: '推荐指数';
  font-size: 9px;
  font-weight: 500;
  line-height: 12px;
  text-align: right;
}

.match-avatar-wrap {
  position: relative;
  width: 116px;
  height: 116px;
  margin: 0 auto 16px;
}

.match-avatar {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border: 4px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  object-fit: cover;
  background: #334155;
  box-shadow: 0 14px 28px rgba(2, 6, 23, 0.38);
}

.match-online {
  position: absolute;
  right: 6px;
  bottom: 8px;
  width: 16px;
  height: 16px;
  box-sizing: border-box;
  border: 3px solid #1e293b;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
}

.match-identity {
  text-align: center;
}

.match-identity h2 {
  margin: 0;
  overflow: hidden;
  color: #f8fafc;
  font-size: 23px;
  font-weight: 800;
  line-height: 31px;
  letter-spacing: -0.025em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-identity p {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin: 5px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.match-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.match-tags span {
  padding: 6px 11px;
  border: 1px solid rgba(96, 165, 250, 0.16);
  border-radius: 999px;
  color: #93c5fd;
  background: rgba(51, 65, 85, 0.84);
  font-size: 11px;
  font-weight: 600;
  line-height: 14px;
}

.match-profile {
  display: -webkit-box;
  margin: 18px 0 0;
  overflow: hidden;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 21px;
  text-align: center;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.match-reasons {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.56);
}

.match-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #60a5fa;
}

.match-section-title h3 {
  margin: 0;
  color: #f8fafc;
  font-size: 14px;
  font-weight: 700;
}

.match-reasons ul {
  display: grid;
  gap: 9px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.match-reasons li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #cbd5e1;
  font-size: 12px;
  line-height: 18px;
}

.match-reasons li svg {
  flex: 0 0 auto;
  margin-top: 1px;
  color: #22c55e;
}

.match-progress {
  display: flex;
  justify-content: center;
  gap: 5px;
  margin-top: 18px;
}

.match-progress span {
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: #475569;
  transition: width 0.2s ease, background 0.2s ease;
}

.match-progress span.active {
  width: 20px;
  background: #3b82f6;
}

.match-actions {
  display: grid;
  width: 100%;
  max-width: 430px;
  margin: 18px auto 0;
  grid-template-columns: 72px minmax(0, 1fr) 72px;
  gap: 12px;
}

.match-action {
  display: flex;
  min-width: 0;
  height: 58px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 0 8px;
  border-radius: 18px;
  font: inherit;
  transition: transform 0.15s ease;
}

.match-action:active {
  transform: scale(0.95);
}

.match-action span {
  font-size: 10px;
  font-weight: 600;
}

.match-action--skip,
.match-action--next {
  border: 1px solid #334155;
  color: #94a3b8;
  background: #1e293b;
}

.match-action--contact {
  border: 0;
  color: #0f172a;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(2, 6, 23, 0.25);
}

.match-empty {
  display: flex;
  width: 100%;
  min-height: 360px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #64748b;
  text-align: center;
}

.match-empty h2 {
  margin: 18px 0 6px;
  color: #f8fafc;
  font-size: 18px;
}

.match-empty p {
  max-width: 280px;
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
  line-height: 20px;
}

.match-empty button {
  display: flex;
  height: 44px;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  color: #0f172a;
  background: #ffffff;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
}

.match-card--loading {
  display: flex;
  min-height: 800px;
  flex-direction: column;
  align-items: center;
}

.loading-pill,
.loading-avatar,
.loading-line,
.loading-tags span {
  background: linear-gradient(90deg, #334155 25%, #475569 50%, #334155 75%);
  background-size: 200% 100%;
  animation: match-shimmer 1.4s infinite linear;
}

.loading-pill {
  width: 92px;
  height: 26px;
  align-self: flex-start;
  border-radius: 999px;
}

.loading-avatar {
  width: 116px;
  height: 116px;
  margin-top: 32px;
  border-radius: 50%;
}

.loading-line {
  height: 14px;
  border-radius: 999px;
}

.loading-line--name {
  width: 45%;
  height: 22px;
  margin-top: 22px;
}

.loading-line--profile {
  width: 72%;
  margin-top: 15px;
}

.loading-tags {
  display: flex;
  gap: 8px;
  margin-top: 24px;
}

.loading-tags span {
  width: 62px;
  height: 26px;
  border-radius: 999px;
}

.spinning {
  animation: match-spin 1s linear infinite;
}

@keyframes match-spin {
  to { transform: rotate(360deg); }
}

.is-spinning, .spinning {
  animation: match-spin 0.8s linear infinite;
}

.match-action:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@keyframes match-shimmer {
  to { background-position: -200% 0; }
}

@media (max-width: 359px) {
  .match-page {
    padding-right: 12px;
    padding-left: 12px;
  }

  .match-card {
    padding-right: 14px;
    padding-left: 14px;
  }
}
</style>

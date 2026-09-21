<template>
  <div class="home-page">
    <header class="home-header">
      <div class="home-header__copy">
        <h1>CodeMate</h1>
        <p>快速发掘和你默契最合的技术拍档</p>
      </div>
      <div class="notice-wrap">
        <button class="notice-button" type="button" aria-label="通知" @click="showNotice">
          <Bell :size="20" :stroke-width="1.8" />
        </button>
        <span class="notice-badge" />
      </div>
    </header>

    <AppSearchBox v-model="searchKeyword" placeholder="输入技术栈、拼音、账号搜索伙伴..." />

    <div class="filter-list" aria-label="伙伴分类">
      <button
        v-for="tag in filterTags"
        :key="tag.name"
        class="filter-button"
        :class="{ 'filter-button--active': activeFilter === tag.name }"
        type="button"
        @click="activeFilter = tag.name"
      >
        {{ tag.name }}
      </button>
    </div>

    <div class="partner-section-heading">
      <h2>推荐列表 <span>({{ filteredUsers.length }}人)</span></h2>
      <button type="button" @click="toggleSort">
        按活跃度排序
        <ArrowDownUp :size="16" :stroke-width="1.8" />
      </button>
    </div>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div v-if="loading && !userList.length" class="partner-list">
        <div v-for="item in 4" :key="item" class="partner-card partner-card--loading">
          <span class="loading-avatar" />
          <div class="loading-lines"><span /><span /><span /></div>
        </div>
      </div>

      <template v-else-if="filteredUsers.length">
        <div class="partner-list">
          <article
            v-for="(user, index) in filteredUsers"
            :key="user.id"
            class="partner-card"
            :class="{ 'partner-card--featured': index === 1 }"
          >
            <div class="partner-avatar-wrap">
              <img
                class="partner-avatar"
                :src="user.avatarUrl || fallbackAvatar"
                :alt="user.username"
                @error="onAvatarError"
              />
            </div>

            <div class="partner-info">
              <div>
                <h3>{{ formatUserName(user) }}</h3>
                <p>寻找项目队友</p>
                <div class="partner-tags">
                  <span v-for="tag in user.tags" :key="tag">{{ tag }}</span>
                </div>
              </div>
              <div class="contact-row">
                <button
                  type="button"
                  :disabled="contactingUserId === user.id"
                  @click.stop="contactUser(user)"
                >
                  <LoaderCircle v-if="contactingUserId === user.id" class="is-spinning" :size="16" />
                  <Send v-else :size="16" :stroke-width="1.9" />
                  <span>{{ contactingUserId === user.id ? '发起中...' : '联系我' }}</span>
                </button>
              </div>
            </div>
          </article>
        </div>

        <!-- 滑动到底部自然停留在第 20 条卡片处的分页状态区 -->
        <div
          class="scroll-load-status"
          :class="{ 'scroll-load-status--finished': finished }"
          @click="!finished && !loadingMore && onLoadNextPage()"
        >
          <template v-if="loadingMore">
            <LoaderCircle class="is-spinning" :size="16" />
            <span>正在加载下 20 位伙伴...</span>
          </template>
          <template v-else-if="finished">
            <span>— 没有更多推荐伙伴了 —</span>
          </template>
          <template v-else>
            <span>上拉或触底加载下 20 位伙伴</span>
          </template>
        </div>
      </template>

      <div v-else class="partner-empty">
        <UsersRound :size="58" :stroke-width="1.3" />
        <p>没有找到符合条件的伙伴</p>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDownUp, Bell, LoaderCircle, Send, UsersRound } from 'lucide-vue-next';
import AppSearchBox from '../components/AppSearchBox.vue';
import { Toast } from 'vant';
import myAxios from '../plugins/myAxios';
import type { UserType } from '../models/user';

const router = useRouter();

type Partner = Omit<UserType, 'tags'> & { tags: string[] };
interface FilterTag { name: string; keywords: string[]; }

const searchKeyword = ref('');
const activeFilter = ref('全部');
const sortDescending = ref(true);
const userList = ref<Partner[]>([]);
const loading = ref(true);
const refreshing = ref(false);
const pageNum = ref(1);
const pageSize = 20;
const total = ref(0);
const loadingMore = ref(false);
const finished = ref(false);
let requestSeq = 0;

const filterTags: FilterTag[] = [
  { name: '全部', keywords: [] },
  { name: '前端开发', keywords: ['vue', 'react', 'javascript', 'typescript', 'html', 'css', 'tailwind', 'nextjs'] },
  { name: '后端技术', keywords: ['java', 'spring', 'mysql', 'go', 'python', 'node', 'redis'] },
  { name: 'AI/深度学习', keywords: ['ai', 'rag', 'pytorch', 'llm', 'deepseek', '机器学习', '深度学习'] },
  { name: '移动端', keywords: ['android', 'ios', 'flutter', 'swift', 'kotlin', 'react native'] },
];
const fallbackAvatar = 'https://api.dicebear.com/8.x/initials/svg?seed=Code&backgroundColor=334155&fontFamily=Arial';

const normalizeTags = (rawTags: unknown): string[] => {
  if (!rawTags) return [];
  if (Array.isArray(rawTags)) return rawTags.map(String);
  try {
    const parsed = JSON.parse(String(rawTags));
    return Array.isArray(parsed) ? parsed.map(String) : [String(parsed)];
  } catch {
    return String(rawTags).split(/[,，]/).map((tag) => tag.trim()).filter(Boolean);
  }
};

const filteredUsers = computed(() => {
  const keyword = searchKeyword.value.toLowerCase();
  const currentFilter = filterTags.find((tag) => tag.name === activeFilter.value);
  const result = userList.value.filter((user) => {
    const lowerTags = user.tags.map((tag) => tag.toLowerCase());
    const matchesFilter = !currentFilter?.keywords.length || currentFilter.keywords.some((filter) => lowerTags.some((tag) => tag.includes(filter)));
    const searchable = [user.username, user.userAccount, user.planetCode, user.profile, ...user.tags].filter(Boolean).join(' ').toLowerCase();
    return matchesFilter && (!keyword || searchable.includes(keyword));
  });
  return sortDescending.value ? result : [...result].reverse();
});

const loadData = async (targetPage = 1, isRefresh = false) => {
  const currentRequestSeq = ++requestSeq;
  if (targetPage === 1) {
    if (!isRefresh && !userList.value.length) {
      loading.value = true;
    }
  } else {
    loadingMore.value = true;
  }

  try {
    const response: any = await myAxios.get('/user/recommend', {
      params: {
        pageSize,
        pageNum: targetPage,
      },
    });
    if (currentRequestSeq !== requestSeq) return;

    const page = response?.data;
    const records: UserType[] = page?.records || [];
    total.value = Number(page?.total) || 0;
    const formattedRecords: Partner[] = records.map((user: UserType) => ({
      ...user,
      tags: normalizeTags(user.tags),
    }));

    if (targetPage === 1) {
      userList.value = formattedRecords;
      pageNum.value = 1;
      finished.value = false;
    } else {
      const existingIds = new Set(userList.value.map((u) => u.id));
      const uniqueNew = formattedRecords.filter((u) => !existingIds.has(u.id));
      userList.value = [...userList.value, ...uniqueNew];
      pageNum.value = targetPage;
    }

    const totalPages = Number(page?.pages) || 0;
    if (
      records.length < pageSize ||
      (totalPages > 0 && targetPage >= totalPages) ||
      (total.value > 0 && userList.value.length >= total.value)
    ) {
      finished.value = true;
    }
  } catch (error) {
    console.error('/user/recommend error', error);
    if (targetPage === 1 && !userList.value.length) {
      userList.value = [];
    }
    Toast.fail(targetPage === 1 ? '请求推荐伙伴失败' : '加载更多伙伴失败');
  } finally {
    if (currentRequestSeq === requestSeq) {
      loading.value = false;
      loadingMore.value = false;
      refreshing.value = false;
    }
  }
};

const onLoadNextPage = async () => {
  if (loading.value || loadingMore.value || finished.value || refreshing.value) {
    return;
  }
  await loadData(pageNum.value + 1, false);
};

const onContentScroll = () => {
  const el = document.getElementById('content');
  if (!el || loading.value || loadingMore.value || finished.value || refreshing.value) {
    return;
  }
  // 严格在滑到底部（距离底端 <= 20px）时才触发下一页，保持前 20 条自然完整展示
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20) {
    onLoadNextPage();
  }
};

onMounted(() => {
  loadData(1);
  const contentEl = document.getElementById('content');
  contentEl?.addEventListener('scroll', onContentScroll, { passive: true });
});

onBeforeUnmount(() => {
  const contentEl = document.getElementById('content');
  contentEl?.removeEventListener('scroll', onContentScroll);
});

const formatUserName = (user: Partner) => user.planetCode ? `${user.username}(${user.planetCode})` : user.username;
const onAvatarError = (event: Event) => { const image = event.target as HTMLImageElement; if (image.src !== fallbackAvatar) image.src = fallbackAvatar; };
const onRefresh = async () => {
  await loadData(1, true);
  Toast.success('刷新成功');
};
const toggleSort = () => { sortDescending.value = !sortDescending.value; };
const showNotice = () => Toast('暂无新通知');
const contactingUserId = ref<number | null>(null);

const contactUser = async (user: Partner) => {
  if (!user || !user.id) {
    Toast.fail('用户信息异常');
    return;
  }
  if (contactingUserId.value !== null) {
    return;
  }
  contactingUserId.value = Number(user.id);
  try {
    const res: any = await myAxios.post('/chat/private/start', {
      targetUserId: Number(user.id),
    });
    if (res?.code === 0 && res?.data) {
      const session = res.data;
      const targetName = session.targetUser?.userName || user.username || '该伙伴';
      const targetAvatar = session.targetUser?.avatarUrl || user.avatarUrl || '';
      router.push({
        path: '/chat/private',
        query: {
          sessionId: String(session.sessionId),
          targetUserId: String(user.id),
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
    contactingUserId.value = null;
  }
};
</script>

<style scoped>
.home-page { width: 100%; max-width: 100%; min-height: 100%; box-sizing: border-box; padding: 30px 16px 24px; overflow-x: hidden; color: #f8fafc; background: #0f172a; }
.home-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.home-header__copy { min-width: 0; }
.home-header h1 { margin: 0; color: #f8fafc; font-size: 24px; font-weight: 700; line-height: 32px; letter-spacing: -.025em; }
.home-header p { margin: 4px 0 0; color: #cbd5e1; font-size: 14px; line-height: 20px; }
.notice-wrap { position: relative; flex: 0 0 auto; }
.notice-button { display: grid; width: 40px; height: 40px; padding: 0; place-items: center; border: 0; border-radius: 50%; color: #94a3b8; background: #1e293b; transition: color .15s ease, background .15s ease, transform .15s ease; }
.notice-button:active { transform: scale(.95); }
.notice-badge { position: absolute; top: 0; right: 0; width: 10px; height: 10px; border: 2px solid #0f172a; border-radius: 50%; background: #ef4444; }
.filter-list { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; padding-bottom: 4px; overflow-x: auto; scrollbar-width: none; }
.filter-list::-webkit-scrollbar { display: none; }
.filter-button { flex: 0 0 auto; padding: 10px 20px; border: 0; border-radius: 999px; color: #cbd5e1; background: #1e293b; font: inherit; font-size: 14px; line-height: 20px; white-space: nowrap; transition: transform .15s ease, background .15s ease, color .15s ease; }
.filter-button:active { transform: scale(.95); }
.filter-button--active { color: #030712; background: #fff; font-weight: 600; }
.partner-section-heading { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 20px; }
.partner-section-heading h2 { margin: 0; color: #f8fafc; font-size: 18px; font-weight: 600; line-height: 28px; }
.partner-section-heading h2 span { color: #94a3b8; font-size: 14px; font-weight: 400; }
.partner-section-heading button { display: flex; align-items: center; gap: 6px; padding: 0; border: 0; color: #f8fafc; background: transparent; font: inherit; font-size: 14px; font-weight: 500; white-space: nowrap; }
.partner-section-heading button svg { color: #94a3b8; }
.home-page :deep(.van-pull-refresh),
.home-page :deep(.van-pull-refresh__track) { width: 100%; min-width: 0; }
.partner-list { display: grid; grid-template-columns: minmax(0, 1fr); justify-items: center; width: 100%; min-width: 0; gap: 16px; }
.partner-card { display: flex; width: 100%; max-width: 100%; min-width: 0; margin-right: auto; margin-left: auto; gap: 16px; min-height: 112px; box-sizing: border-box; overflow: hidden; padding: 16px; border: 1px solid transparent; border-radius: 16px; background: #1e293b; transition: transform .15s ease, box-shadow .15s ease; }
.partner-card--featured { border-color: rgba(255,255,255,.2); box-shadow: 0 10px 22px rgba(2,6,23,.22); }
.partner-avatar-wrap { position: relative; flex: 0 0 auto; width: 80px; height: 80px; }
.partner-avatar { display: block; width: 80px; height: 80px; border-radius: 12px; object-fit: cover; background: #334155; }
.partner-info { display: flex; width: 0; min-width: 0; flex: 1 1 0; flex-direction: column; justify-content: space-between; }
.partner-info > div { min-width: 0; }
.partner-info h3 { width: 100%; max-width: 100%; margin: 0; overflow: hidden; color: #f8fafc; font-size: 16px; font-weight: 600; line-height: 20px; text-overflow: ellipsis; white-space: nowrap; }
.partner-info p { margin: 4px 0 0; color: #cbd5e1; font-size: 12px; line-height: 16px; }
.partner-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.partner-tags span { padding: 4px 12px; border-radius: 999px; color: #cbd5e1; background: #334155; font-size: 12px; line-height: 16px; }
.contact-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.contact-row button { display: inline-flex; max-width: 100%; box-sizing: border-box; align-items: center; gap: 8px; padding: 8px 20px; border: 0; border-radius: 999px; color: #030712; background: #fff; font: inherit; font-size: 14px; font-weight: 600; line-height: 20px; box-shadow: 0 4px 8px rgba(2,6,23,.18); transition: transform .15s ease, background .15s ease; }
.contact-row button:active { transform: scale(.95); background: #f4f4f5; }
.partner-empty { padding: 64px 20px; color: #94a3b8; text-align: center; }
.partner-empty svg { display: block; margin: 0 auto 16px; opacity: .2; }
.partner-empty p { margin: 0; font-size: 14px; }
.partner-card--loading { min-height: 144px; }
.loading-avatar { flex: 0 0 80px; width: 80px; height: 80px; border-radius: 12px; background: #334155; animation: loading-pulse 1.4s ease-in-out infinite; }
.loading-lines { display: grid; align-content: start; gap: 10px; flex: 1; padding-top: 4px; }
.loading-lines span { display: block; height: 10px; border-radius: 999px; background: #334155; animation: loading-pulse 1.4s ease-in-out infinite; }
.loading-lines span:nth-child(2) { width: 72%; }
.loading-lines span:nth-child(3) { width: 48%; }
@keyframes pulse-green { 0%,100% { opacity: 1; } 50% { opacity: .5; } }
@keyframes loading-pulse { 0%,100% { opacity: .5; } 50% { opacity: 1; } }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.is-spinning { animation: spin 0.8s linear infinite; }
.contact-row button:disabled { opacity: 0.65; cursor: not-allowed; }
.scroll-load-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  box-sizing: border-box;
  padding: 20px 16px 28px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 20px;
  text-align: center;
  user-select: none;
}
.scroll-load-status .is-spinning {
  color: #38bdf8;
}
.scroll-load-status--finished {
  color: #64748b;
  font-size: 12px;
}
</style>

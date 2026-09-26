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

    <AppSearchBox trigger placeholder="按标签搜索技术伙伴..." @activate="goSearchPage" />

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

    <PartnerCardList v-if="loading && !userList.length">
      <PartnerCard v-for="item in 4" :key="item" loading />
    </PartnerCardList>

    <template v-else-if="filteredUsers.length">
      <PartnerCardList>
        <PartnerCard
            v-for="user in filteredUsers"
            :key="user.id"
            :user="user"
            :contacted="isUserContacted(user.id)"
            :contacting="contactingUserId === user.id"
            @contact="contactUser"
        />
      </PartnerCardList>

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
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDownUp, Bell, LoaderCircle, UsersRound } from 'lucide-vue-next';
import AppSearchBox from '../components/AppSearchBox.vue';
import PartnerCard from '../components/PartnerCard.vue';
import PartnerCardList from '../components/PartnerCardList.vue';
import { Toast } from 'vant';
import myAxios from '../plugins/myAxios';
import type { UserType } from '../models/user';
import { getContactedUserIds, markUserContacted } from '../services/privateChat';

const router = useRouter();

type Partner = Omit<UserType, 'tags'> & { tags: string[] };
interface FilterTag { name: string; keywords: string[]; }

const activeFilter = ref('全部');
const sortDescending = ref(true);
const userList = ref<Partner[]>([]);
const loading = ref(true);
const pageNum = ref(1);
const pageSize = 20;
const total = ref(0);
const loadingMore = ref(false);
const finished = ref(false);
const contactedUserIds = ref<number[]>(getContactedUserIds());
let requestSeq = 0;

const filterTags: FilterTag[] = [
  { name: '全部', keywords: [] },
  { name: '前端开发', keywords: ['vue', 'react', 'javascript', 'typescript', 'html', 'css', 'tailwind', 'nextjs'] },
  { name: '后端技术', keywords: ['java', 'spring', 'mysql', 'go', 'python', 'node', 'redis'] },
  { name: 'AI/深度学习', keywords: ['ai', 'rag', 'pytorch', 'llm', 'deepseek', '机器学习', '深度学习'] },
  { name: '移动端', keywords: ['android', 'ios', 'flutter', 'swift', 'kotlin', 'react native'] },
];
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
  const currentFilter = filterTags.find((tag) => tag.name === activeFilter.value);
  const result = userList.value.filter((user) => {
    const lowerTags = user.tags.map((tag) => tag.toLowerCase());
    const matchesFilter = !currentFilter?.keywords.length || currentFilter.keywords.some((filter) => lowerTags.some((tag) => tag.includes(filter)));
    return matchesFilter;
  });
  return sortDescending.value ? result : [...result].reverse();
});

const loadData = async (targetPage = 1) => {
  const currentRequestSeq = ++requestSeq;
  if (targetPage === 1) {
    if (!userList.value.length) {
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
    }
  }
};

const onLoadNextPage = async () => {
  if (loading.value || loadingMore.value || finished.value) {
    return;
  }
  await loadData(pageNum.value + 1);
};

const onContentScroll = () => {
  const el = document.getElementById('content');
  if (!el || loading.value || loadingMore.value || finished.value) {
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

const toggleSort = () => { sortDescending.value = !sortDescending.value; };
const showNotice = () => Toast('暂无新通知');
const goSearchPage = () => router.push('/search');
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
      markUserContacted(user.id);
      contactedUserIds.value = getContactedUserIds();
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

const isUserContacted = (userId: number) => contactedUserIds.value.includes(Number(userId));
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
.partner-empty { padding: 64px 20px; color: #94a3b8; text-align: center; }
.partner-empty svg { display: block; margin: 0 auto 16px; opacity: .2; }
.partner-empty p { margin: 0; font-size: 14px; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.is-spinning { animation: spin 0.8s linear infinite; }
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

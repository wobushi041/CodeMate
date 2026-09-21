<template>
  <div id="teamPage" class="team-page">
    <header class="team-header">
      <div class="team-title-row">
        <div class="team-title-copy">
          <h1>找队伍</h1>
          <p>发现并加入适合你的技术队伍</p>
        </div>
      </div>
      <label class="team-search">
        <Search :size="18" :stroke-width="2" />
        <input v-model.trim="searchText" type="search" placeholder="搜索队伍" @keyup.enter="onSearch(searchText)" />
        <button v-if="searchText" type="button" aria-label="清除搜索" @click="clearSearch">×</button>
      </label>
      <div class="team-filter-tabs" role="tablist" aria-label="队伍类型">
        <button type="button" :class="{ active: active === 'public' }" @click="onTabChange('public')"><Globe :size="16" />公开</button>
        <button type="button" :class="{ active: active === 'private' }" @click="onTabChange('private')"><LockKeyhole :size="16" />加密</button>
      </div>
    </header>

    <main class="team-list-panel">
      <!-- 骨架屏加载态 -->
      <div v-if="loading" class="team-skeleton-list">
        <div v-for="item in 3" :key="item" class="team-skeleton-card">
          <div class="team-skeleton-header">
            <div class="team-skeleton-heading">
              <span class="skeleton-block skeleton-title" />
              <span class="skeleton-block skeleton-desc" />
            </div>
            <span class="skeleton-block skeleton-badge" />
          </div>
          <div class="team-skeleton-details">
            <div class="skeleton-row">
              <span class="skeleton-block skeleton-icon" />
              <span class="skeleton-block skeleton-text skeleton-text--short" />
            </div>
            <div class="skeleton-row">
              <span class="skeleton-block skeleton-icon" />
              <span class="skeleton-block skeleton-text skeleton-text--medium" />
            </div>
            <div class="skeleton-row">
              <span class="skeleton-block skeleton-icon" />
              <span class="skeleton-block skeleton-text skeleton-text--medium" />
            </div>
          </div>
          <div class="team-skeleton-actions">
            <span class="skeleton-block skeleton-button" />
            <span class="skeleton-block skeleton-button" />
          </div>
        </div>
      </div>

      <!-- 队伍真实列表 -->
      <team-card-list v-else-if="teamList?.length > 0" :key="active" :team-list="teamList" @refresh="refreshTeamList" />

      <!-- 空状态 -->
      <div v-else class="team-empty">
        <UsersRound :size="54" :stroke-width="1.4" />
        <h2>没有找到符合条件的队伍</h2>
        <p>换个关键词或筛选条件再试试吧</p>
      </div>
    </main>

    <button class="team-create-button" type="button" aria-label="创建队伍" @click="toAddTeam">
      <Plus :size="28" :stroke-width="1.8" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Globe, LockKeyhole, Plus, Search, UsersRound } from 'lucide-vue-next';
import { Toast } from 'vant';
import TeamCardList from '../components/TeamCardList.vue';
import type { TeamType } from '../models/team';
import myAxios from '../plugins/myAxios';

const active = ref('public');
const router = useRouter();
const searchText = ref('');
const teamList = ref<TeamType[]>([]);
const loading = ref(true);

const onTabChange = (name: string) => {
  active.value = name;
  listTeam(searchText.value, name === 'public' ? 0 : 2);
};

const toAddTeam = () => router.push({ path: '/team/add' });
const refreshTeamList = () => listTeam(searchText.value, active.value === 'public' ? 0 : 2);

const listTeam = async (val = '', status = 0) => {
  loading.value = true;
  try {
    const res: any = await myAxios.get('/team/list', { params: { searchText: val, pageNum: 1, status } });
    if (res?.code === 0) {
      teamList.value = res.data;
    } else {
      Toast.fail('加载队伍失败，请刷新重试');
    }
  } catch (error) {
    Toast.fail('加载队伍失败，请刷新重试');
  } finally {
    loading.value = false;
  }
};

const onSearch = (val = '') => listTeam(val, active.value === 'public' ? 0 : 2);
const clearSearch = () => { searchText.value = ''; onSearch(); };
onMounted(() => refreshTeamList());
</script>

<style scoped>
.team-page{height:100%;min-height:0;box-sizing:border-box;display:flex;flex-direction:column;overflow:hidden;background:#0f172a;color:#f8fafc}.team-header{flex:0 0 auto;padding:30px 16px 16px;background:linear-gradient(180deg,#0f172a 0%,rgba(15,23,42,.95) 86%,rgba(15,23,42,0) 100%)}.team-title-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px}.team-title-copy{min-width:0}.team-title-row h1{margin:0;color:#f8fafc;font-size:24px;font-weight:700;line-height:32px;letter-spacing:-.025em}.team-title-copy p{margin:4px 0 0;color:#cbd5e1;font-size:14px;line-height:20px}.team-create-button{position:fixed;right:16px;bottom:calc(64px + env(safe-area-inset-bottom) + 16px);z-index:90;display:grid;width:52px;height:52px;padding:0;place-items:center;border:0;border-radius:50%;color:#fff;background:#3b82f6;box-shadow:0 10px 24px rgba(37,99,235,.38);transition:transform .15s ease,background-color .15s ease}.team-create-button:active,.team-filter-tabs button:active{transform:scale(.95)}.team-search{display:flex;align-items:center;gap:10px;height:50px;box-sizing:border-box;padding:0 15px;border:1px solid rgba(148,163,184,.18);border-radius:17px;color:#64748b;background:#1e293b}.team-search input{min-width:0;flex:1;border:0;outline:0;color:#f8fafc;background:transparent;font:inherit;font-size:13px}.team-search input::placeholder{color:#64748b}.team-search button{padding:0;border:0;color:#94a3b8;background:transparent;font-size:22px;line-height:1}.team-filter-tabs{display:flex;gap:5px;margin-top:16px;padding:4px;border:1px solid rgba(148,163,184,.16);border-radius:999px;background:#1e293b}.team-filter-tabs button{display:flex;flex:1;align-items:center;justify-content:center;gap:7px;height:36px;border:0;border-radius:999px;color:#94a3b8;background:transparent;font-size:13px;transition:transform .15s ease,background .2s ease,color .2s ease}.team-filter-tabs button.active{color:#fff;background:#3b82f6;box-shadow:0 5px 14px rgba(59,130,246,.22)}.team-list-panel{min-height:0;flex:1;overflow-y:auto;overflow-x:hidden;padding-bottom:86px;-webkit-overflow-scrolling:touch}.team-empty{padding:70px 20px;text-align:center;color:#64748b}.team-empty svg{display:block;margin:0 auto 16px;opacity:.35}.team-empty h2{margin:0;color:#cbd5e1;font-size:16px;font-weight:600}.team-empty p{margin:7px 0 0;font-size:12px}

/* 队伍骨架屏样式 */
.team-skeleton-list {
  box-sizing: border-box;
  display: grid;
  gap: 14px;
  padding: 0 12px 18px;
}

.team-skeleton-card {
  box-sizing: border-box;
  padding: 17px 15px 16px;
  overflow: hidden;
  border: 1px solid rgba(100, 116, 139, 0.32);
  border-radius: 18px;
  background: #1e293b;
  box-shadow: 0 8px 20px rgba(2, 6, 23, 0.08);
}

.team-skeleton-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 14px;
}

.team-skeleton-heading {
  min-width: 0;
  flex: 1;
}

.skeleton-block {
  display: block;
  background: #334155;
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}

.skeleton-title {
  height: 18px;
  width: 46%;
  border-radius: 6px;
}

.skeleton-desc {
  height: 12px;
  width: 72%;
  margin-top: 8px;
  border-radius: 4px;
}

.skeleton-badge {
  width: 50px;
  height: 24px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.team-skeleton-details {
  display: grid;
  gap: 9px;
  padding: 13px 0 15px;
  border-top: 1px solid rgba(100, 116, 139, 0.25);
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.skeleton-icon {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex: 0 0 16px;
}

.skeleton-text {
  height: 12px;
  border-radius: 4px;
}

.skeleton-text--short {
  width: 32%;
}

.skeleton-text--medium {
  width: 58%;
}

.team-skeleton-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 13px;
  border-top: 1px solid rgba(100, 116, 139, 0.25);
}

.skeleton-button {
  width: 76px;
  height: 32px;
  border-radius: 999px;
}

@keyframes skeleton-pulse {
  0%, 100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
</style>





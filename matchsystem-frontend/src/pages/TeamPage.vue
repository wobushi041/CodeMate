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
      <team-card-list v-if="teamList?.length > 0" :key="active" :team-list="teamList" @refresh="refreshTeamList" />
      <div v-else class="team-empty"><UsersRound :size="54" :stroke-width="1.4" /><h2>没有找到符合条件的队伍</h2><p>换个关键词或筛选条件再试试吧</p></div>
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

const onTabChange = (name: string) => {
  active.value = name;
  listTeam(searchText.value, name === 'public' ? 0 : 2);
};

const toAddTeam = () => router.push({ path: '/team/add' });
const refreshTeamList = () => listTeam(searchText.value, active.value === 'public' ? 0 : 2);

const listTeam = async (val = '', status = 0) => {
  const res = await myAxios.get('/team/list', { params: { searchText: val, pageNum: 1, status } });
  if (res?.code === 0) teamList.value = res.data;
  else Toast.fail('加载队伍失败，请刷新重试');
};

const onSearch = (val = '') => listTeam(val, active.value === 'public' ? 0 : 2);
const clearSearch = () => { searchText.value = ''; onSearch(); };
onMounted(() => refreshTeamList());
</script>

<style scoped>
.team-page{height:100%;min-height:0;box-sizing:border-box;display:flex;flex-direction:column;overflow:hidden;background:#0f172a;color:#f8fafc}.team-header{flex:0 0 auto;padding:30px 16px 16px;background:linear-gradient(180deg,#0f172a 0%,rgba(15,23,42,.95) 86%,rgba(15,23,42,0) 100%)}.team-title-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px}.team-title-copy{min-width:0}.team-title-row h1{margin:0;color:#f8fafc;font-size:24px;font-weight:700;line-height:32px;letter-spacing:-.025em}.team-title-copy p{margin:4px 0 0;color:#cbd5e1;font-size:14px;line-height:20px}.team-create-button{position:fixed;right:16px;bottom:calc(64px + env(safe-area-inset-bottom) + 16px);z-index:90;display:grid;width:52px;height:52px;padding:0;place-items:center;border:0;border-radius:50%;color:#fff;background:#3b82f6;box-shadow:0 10px 24px rgba(37,99,235,.38);transition:transform .15s ease,background-color .15s ease}.team-create-button:active,.team-filter-tabs button:active{transform:scale(.95)}.team-search{display:flex;align-items:center;gap:10px;height:50px;box-sizing:border-box;padding:0 15px;border:1px solid rgba(148,163,184,.18);border-radius:17px;color:#64748b;background:#1e293b}.team-search input{min-width:0;flex:1;border:0;outline:0;color:#f8fafc;background:transparent;font:inherit;font-size:13px}.team-search input::placeholder{color:#64748b}.team-search button{padding:0;border:0;color:#94a3b8;background:transparent;font-size:22px;line-height:1}.team-filter-tabs{display:flex;gap:5px;margin-top:16px;padding:4px;border:1px solid rgba(148,163,184,.16);border-radius:999px;background:#1e293b}.team-filter-tabs button{display:flex;flex:1;align-items:center;justify-content:center;gap:7px;height:36px;border:0;border-radius:999px;color:#94a3b8;background:transparent;font-size:13px;transition:transform .15s ease,background .2s ease,color .2s ease}.team-filter-tabs button.active{color:#fff;background:#3b82f6;box-shadow:0 5px 14px rgba(59,130,246,.22)}.team-list-panel{min-height:0;flex:1;overflow-y:auto;overflow-x:hidden;padding-bottom:86px;-webkit-overflow-scrolling:touch}.team-empty{padding:70px 20px;text-align:center;color:#64748b}.team-empty svg{display:block;margin:0 auto 16px;opacity:.35}.team-empty h2{margin:0;color:#cbd5e1;font-size:16px;font-weight:600}.team-empty p{margin:7px 0 0;font-size:12px}
</style>





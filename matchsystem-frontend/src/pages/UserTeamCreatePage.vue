<template>
  <SubPageLayout title="我创建的队伍" back-path="/user">
    <AppSearchBox v-model="searchText" class="team-search" placeholder="输入队伍名称搜索..." @search="onSearch" />

    <!-- 骨架屏 -->
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

    <!-- 真实队伍列表 -->
    <team-card-list v-else-if="teamList?.length > 0" :team-list="teamList" @refresh="listTeam(searchText)" />

    <!-- 空状态 -->
    <div v-else class="team-empty">
      <UsersRound :size="54" :stroke-width="1.4" />
      <h2>暂无创建的队伍</h2>
      <p>点击下方“创建队伍”开始招募伙伴吧</p>
    </div>

    <template #bottom>
      <button class="mall-action-btn" type="button" @click="doJoinTeam">
        <span>创建队伍</span>
      </button>
    </template>
  </SubPageLayout>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import TeamCardList from "../components/TeamCardList.vue";
import SubPageLayout from "../components/SubPageLayout.vue";
import { onMounted, ref } from "vue";
import myAxios from "../plugins/myAxios";
import { Toast } from "vant";
import AppSearchBox from "../components/AppSearchBox.vue";
import { UsersRound } from 'lucide-vue-next';

const router = useRouter();
const searchText = ref('');
const teamList = ref([]);
const loading = ref(true);

// 跳转到加入队伍页
const doJoinTeam = () => {
  router.push({
    path: "/team/add"
  });
};

const listTeam = async (val = '') => {
  loading.value = true;
  try {
    const res: any = await myAxios.get("/team/list/my/create", {
      params: {
        searchText: val,
        pageNum: 1,
      },
    });
    if (res?.code === 0) {
      teamList.value = res.data || [];
    } else {
      Toast.fail('加载队伍失败，请刷新重试');
    }
  } catch (error) {
    console.error('/team/list/my/create error', error);
    Toast.fail('加载队伍失败，请刷新重试');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  listTeam();
});

const onSearch = (val: string) => {
  listTeam(val);
};
</script>

<style scoped>
.team-search {
  width: calc(100% - 32px);
  margin: 0 16px 12px;
}

.mall-action-btn {
  display: flex;
  width: 100%;
  height: 52px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: #3b82f6;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.3);
  transition: transform 0.15s ease, background 0.15s ease;
}

.mall-action-btn:active {
  transform: scale(0.98);
  background: #2563eb;
}

/* 队伍骨架屏样式 */
.team-skeleton-list {
  box-sizing: border-box;
  display: grid;
  gap: 14px;
  padding: 0 16px 18px;
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

.team-empty {
  padding: 60px 20px;
  text-align: center;
  color: #64748b;
}

.team-empty svg {
  display: block;
  margin: 0 auto 16px;
  opacity: 0.35;
}

.team-empty h2 {
  margin: 0;
  color: #cbd5e1;
  font-size: 16px;
  font-weight: 600;
}

.team-empty p {
  margin: 7px 0 0;
  font-size: 12px;
}
</style>

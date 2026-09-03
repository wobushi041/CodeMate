<template>
  <div id="teamPage" class="mall-page team-page">
    <div class="team-toolbar">
      <van-search v-model="searchText" placeholder="搜索队伍" @search="onSearch" />
      <van-tabs
          v-model:active="active"
          class="team-filter-tabs"
          :line-width="0"
          @change="onTabChange"
      >
        <van-tab title="公开" name="public" />
        <van-tab title="加密" name="private" />
      </van-tabs>
    </div>
    <van-button class="add-button mall-add-btn" type="primary" icon="plus" @click="toAddTeam" />
    <div class="team-list-panel">
      <team-card-list
          v-if="teamList?.length > 0"
          :key="active"
          :teamList="teamList"
          @refresh="refreshTeamList"
      />
      <van-empty v-else description="数据为空"/>
    </div>
  </div>
</template>

<script setup lang="ts">

import {useRouter} from "vue-router";
import TeamCardList from "../components/TeamCardList.vue";
import {onMounted, ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";

const active = ref('public')
const router = useRouter();
const searchText = ref('');

/**
 * 切换查询状态
 * @param name
 */
const onTabChange = (name) => {
  // 查公开
  if (name === 'public') {
    listTeam(searchText.value, 0);
  } else {
    // 查加密
    listTeam(searchText.value, 2);
  }
}

// 跳转到创建队伍页
const toAddTeam = () => {
  router.push({
    path: "/team/add"
  })
}

const teamList = ref([]);

const refreshTeamList = () => {
  const status = active.value === 'public' ? 0 : 2;
  listTeam(searchText.value, status);
}

/**
 * 搜索队伍
 * @param val
 * @param status
 * @returns {Promise<void>}
 */
const listTeam = async (val = '', status = 0) => {
  const res = await myAxios.get("/team/list", {
    params: {
      searchText: val,
      pageNum: 1,
      status,
    },
  });
  if (res?.code === 0) {
    teamList.value = res.data;
  } else {
    Toast.fail('加载队伍失败，请刷新重试');
  }
}

// 页面加载时只触发一次
onMounted( () => {
  refreshTeamList();
})

const onSearch = (val) => {
  listTeam(val, active.value === 'public' ? 0 : 2);
};

</script>

<style scoped>
.team-page {
  height: 100%;
  min-height: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.team-toolbar {
  flex: 0 0 auto;
  padding: 10px 12px 12px;
  background: rgba(39, 42, 55, 0.72);
  border-bottom: 1px solid var(--border-weak);
}

.team-toolbar :deep(.van-search) {
  box-sizing: border-box;
  padding: 0;
  background: transparent;
}

.team-toolbar :deep(.van-search__content) {
  box-sizing: border-box;
  height: 40px;
  padding-left: 12px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
}

.team-toolbar :deep(.van-tabs__wrap) {
  height: 38px;
  margin-top: 10px;
}

.team-filter-tabs :deep(.van-tabs__nav) {
  box-sizing: border-box;
  gap: 6px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 999px;
}

.team-filter-tabs :deep(.van-tab) {
  min-width: 0;
  height: 30px;
  color: var(--text-light);
  border-radius: 999px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.team-filter-tabs :deep(.van-tab--active) {
  color: #fff;
  background: var(--color-primary);
  font-weight: 600;
}

.team-filter-tabs :deep(.van-tabs__line) {
  display: none;
}

.team-list-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 86px;
  -webkit-overflow-scrolling: touch;
}

.team-list-panel :deep(.van-empty) {
  padding-top: 96px;
}

/* 悬浮创建按钮:MallChat 主题蓝 */
.mall-add-btn {
  position: fixed;
  right: 18px;
  bottom: 78px;
  width: 48px;
  height: 48px;
  z-index: 20;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(29, 144, 245, 0.35);
}
</style>

<template>
  <div class="mall-page index-page">
    <van-cell class="mall-match-cell" center title="匹配模式">
      <template #right-icon>
        <van-switch v-model="isMatchMode" size="24" />
      </template>
    </van-cell>
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <user-card-list :user-list="userList" :loading="loading"/>
      <van-empty v-if="!userList || userList.length < 1" description="数据为空"/>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue';
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import UserCardList from "../components/UserCardList.vue";
import {UserType} from "../models/user";

const isMatchMode = ref<boolean>(false);

const userList = ref([]);
const loading = ref(true);
const refreshing = ref(false);

const normalizeTags = (rawTags) => {
  if (!rawTags) {
    return [];
  }
  if (Array.isArray(rawTags)) {
    return rawTags;
  }
  try {
    const parsedTags = JSON.parse(rawTags);
    return Array.isArray(parsedTags) ? parsedTags : [String(parsedTags)];
  } catch (error) {
    return String(rawTags)
      .split(/[,，]/)
      .map(tag => tag.trim())
      .filter(Boolean);
  }
};

/**
 * 加载数据
 */
const loadData = async () => {
  let userListData;
  loading.value = true;
  // 心动模式，根据标签匹配用户
  if (isMatchMode.value) {
    const num = 30;
    userListData = await myAxios.get('/user/match', {
      params: {
        num,
      },
    })
        .then(function (response) {
          console.log('/user/match succeed', response);
          return response?.data;
        })
        .catch(function (error) {
          console.error('/user/match error', error);
          Toast.fail('请求失败');
        })
  } else {
    // 普通模式，直接分页查询用户
    userListData = await myAxios.get('/user/recommend', {
      params: {
        pageSize: 50,
        pageNum: 1,
      },
    })
        .then(function (response) {
          console.log('/user/recommend succeed', response);
          return response?.data?.records;
        })
        .catch(function (error) {
          console.error('/user/recommend error', error);
          Toast.fail('请求失败');
        })
  }
  if (userListData) {
    userListData.forEach((user: UserType) => {
      user.tags = normalizeTags(user.tags);
    })
    userList.value = userListData;
  }
  loading.value = false;
}

watchEffect(() => {
  loadData();
})

const onRefresh = async () => {
  await loadData();
  refreshing.value = false;
  Toast.success('刷新成功');
}

</script>

<style scoped>
.index-page {
  box-sizing: border-box;
  padding-top: 8px;
}

.mall-match-cell {
  box-sizing: border-box;
  width: calc(100% - 24px);
  margin: 0 12px 4px;
  background: var(--bg-panel);
  border: 1px solid var(--border-weak);
  border-radius: 12px;
}

.mall-match-cell :deep(.van-cell__title) {
  color: var(--text-main);
}
</style>

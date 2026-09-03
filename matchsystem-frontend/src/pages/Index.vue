<template>
  <div class="mall-page index-page">
    <van-form class="index-filter-form">
      <van-cell-group class="index-filter-panel" inset>
        <van-field class="index-filter-field" label="匹配模式" readonly>
          <template #input>
            <van-switch v-model="isMatchMode" size="22" />
          </template>
        </van-field>
        <van-field v-if="!isMatchMode" class="index-filter-field" label="推荐人数" readonly>
          <template #input>
            <van-stepper
              v-model="recommendPageSize"
              integer
              :min="1"
              :max="50"
              button-size="24"
              input-width="36"
            />
          </template>
        </van-field>
      </van-cell-group>
    </van-form>
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <user-card-list :user-list="userList" :loading="loading"/>
      <van-empty v-if="!userList || userList.length < 1" description="数据为空"/>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import UserCardList from "../components/UserCardList.vue";
import {UserType} from "../models/user";

const isMatchMode = ref<boolean>(false);
const recommendPageSize = ref(20);

const userList = ref([]);
const loading = ref(true);
const refreshing = ref(false);
let requestSeq = 0;

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
  const currentRequestSeq = ++requestSeq;
  let userListData;
  loading.value = true;
  // 心动模式，根据标签匹配用户
  if (isMatchMode.value) {
    userListData = await myAxios.get('/user/match', {
      params: {
        num: 20,
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
        pageSize: recommendPageSize.value,
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
  if (currentRequestSeq !== requestSeq) {
    return;
  }
  if (userListData) {
    userListData.forEach((user: UserType) => {
      user.tags = normalizeTags(user.tags);
    })
    userList.value = userListData;
  }
  loading.value = false;
}

watch([isMatchMode, recommendPageSize], () => {
  loadData();
}, {immediate: true})

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

.index-filter-form,
.index-filter-panel,
.index-filter-field {
  box-sizing: border-box;
}

.index-filter-form {
  padding: 0 12px 8px;
}

.index-filter-panel {
  box-sizing: border-box;
  width: 100%;
  margin: 0;
  background: var(--bg-panel);
  border: 1px solid var(--border-weak);
  border-radius: 12px;
  overflow: hidden;
}

.index-filter-field {
  background: transparent;
}

.index-filter-field :deep(.van-field__label),
.index-filter-field :deep(.van-field__control) {
  color: var(--text-main);
}

.index-filter-field :deep(.van-field__body) {
  justify-content: flex-end;
}

.index-filter-field :deep(.van-stepper__input) {
  background: var(--bg-elevated);
  color: var(--text-main);
}

.index-filter-field :deep(.van-stepper__minus),
.index-filter-field :deep(.van-stepper__plus) {
  background: rgba(29, 144, 245, 0.16);
  color: #7cc0ff;
}
</style>

<template>
  <van-skeleton title avatar :row="3" :loading="props.loading" class="mall-user-card" v-for="user in props.userList">
    <van-card
        class="mall-card"
        :desc="user.profile"
        :title="`${user.username}(${user.planetCode})`"
        :thumb="user.avatarUrl"
    >
      <template #tags>
        <van-tag plain type="primary" v-for="tag in user.tags" style="margin-right: 8px; margin-top: 8px">
          {{ tag }}
        </van-tag>
      </template>
      <template #footer>
        <van-button size="mini" plain type="primary">联系我</van-button>
      </template>
    </van-card>
  </van-skeleton>
</template>

<script setup lang="ts">
import {UserType} from "../models/user";

interface UserCardListProps {
  loading: boolean;
  userList: UserType[];
}

const props = withDefaults(defineProps<UserCardListProps>(), {
  loading: true,
  // @ts-ignore
  userList: [] as UserType[],
});

</script>

<style scoped>
/* 用户卡片:MallChat 深色卡片质感 */
.mall-user-card {
  box-sizing: border-box;
  display: block;
  width: 100%;
  padding: 0 12px;
  margin-bottom: 12px;
}

.mall-user-card :deep(.van-card) {
  box-sizing: border-box;
  background: var(--bg-elevated);
  border: 1px solid var(--border-weak);
  border-radius: 12px;
}

.mall-user-card :deep(.van-card__header) {
  align-items: flex-start;
}

.mall-user-card :deep(.van-card__thumb) {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
}

.mall-user-card :deep(.van-card__thumb img),
.mall-user-card :deep(.van-card__thumb .van-image) {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  object-fit: cover;
}

.mall-user-card :deep(.van-card__title) {
  color: var(--text-main);
  font-size: 15px;
  font-weight: 600;
}

.mall-user-card :deep(.van-card__desc) {
  display: -webkit-box;
  margin-top: 4px;
  overflow: hidden;
  color: var(--text-light);
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 标签:仿 MallChat 低饱和标签 */
.mall-user-card :deep(.van-tag--primary) {
  background: rgba(29, 144, 245, 0.18);
  color: #7cc0ff;
  border-color: rgba(29, 144, 245, 0.35);
  border-radius: 6px;
  font-weight: 400;
}

.mall-user-card :deep(.van-button--primary) {
  background: rgba(29, 144, 245, 0.16);
  color: #7cc0ff;
  border-color: rgba(29, 144, 245, 0.4);
  border-radius: 8px;
}

/* 骨架屏:铺在同样面板上避免闪白 */
.mall-user-card :deep(.van-skeleton__content) {
  padding: 12px;
}
</style>

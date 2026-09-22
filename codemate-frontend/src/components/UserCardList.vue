<template>
  <div class="mall-user-card" v-for="user in props.userList" :key="user.id">
    <van-skeleton title avatar :row="2" :loading="props.loading">
      <van-card
        class="mall-card"
        :desc="user.profile"
        :title="`${user.username}(${user.planetCode})`"
        :thumb="user.avatarUrl"
      >
        <template #tags>
          <van-tag class="mall-user-tag" plain type="primary" v-for="tag in user.tags" :key="tag">
            {{ tag }}
          </van-tag>
        </template>
        <template #footer>
          <van-button
            size="mini"
            plain
            type="primary"
            :class="{ 'contact-button--contacted': isUserContacted(user.id) }"
            :loading="contactingUserId === user.id"
            @click="contactUser(user)"
          >
            {{ isUserContacted(user.id) ? '已联系' : '联系我' }}
          </van-button>
        </template>
      </van-card>
    </van-skeleton>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import {useRouter} from 'vue-router';
import {Toast} from 'vant';
import myAxios from '../plugins/myAxios';
import {UserType} from "../models/user";
import {getContactedUserIds, markUserContacted} from '../services/privateChat';

interface UserCardListProps {
  loading: boolean;
  userList: UserType[];
}

const props = withDefaults(defineProps<UserCardListProps>(), {
  loading: true,
  // @ts-ignore
  userList: [] as UserType[],
});

const router = useRouter();
const contactedUserIds = ref<number[]>(getContactedUserIds());
const contactingUserId = ref<number | null>(null);
const isUserContacted = (userId: number) => contactedUserIds.value.includes(Number(userId));

const contactUser = async (user: UserType) => {
  if (!user?.id || contactingUserId.value !== null) return;
  contactingUserId.value = Number(user.id);
  try {
    const res: any = await myAxios.post('/chat/private/start', {targetUserId: Number(user.id)});
    if (res?.code !== 0 || !res?.data) {
      Toast.fail(res?.description || res?.message || '发起私聊失败');
      return;
    }
    const session = res.data;
    markUserContacted(user.id);
    contactedUserIds.value = getContactedUserIds();
    router.push({
      path: '/chat/private',
      query: {
        sessionId: String(session.sessionId),
        targetUserId: String(user.id),
        targetUsername: session.targetUser?.userName || user.username || '',
        targetAvatarUrl: session.targetUser?.avatarUrl || user.avatarUrl || '',
        isOnline: session.isTargetOnline ? '1' : '0',
      },
    });
  } catch {
    Toast.fail('请求失败，请稍后重试');
  } finally {
    contactingUserId.value = null;
  }
};

</script>

<style scoped>
/* 用户卡片:MallChat 深色卡片质感 */
.mall-user-card {
  box-sizing: border-box;
  display: block;
  width: 100%;
  padding: 0 8px;
  margin-bottom: 8px;
}

.mall-card,
.mall-user-card :deep(.van-card) {
  box-sizing: border-box;
  width: 100%;
  min-height: 128px;
  margin: 0;
  padding: 14px 68px 14px 16px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(124, 192, 255, 0.12);
  border-radius: 24px !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.mall-user-card :deep(.van-card__header) {
  align-items: center;
  min-height: 100px;
}

.mall-user-card :deep(.van-card__thumb) {
  flex: 0 0 100px;
  width: 100px;
  height: 100px;
  margin-right: 12px;
  border: 1px solid rgba(124, 192, 255, 0.16);
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.06);
}

.mall-user-card :deep(.van-card__content) {
  justify-content: flex-start;
  min-width: 0;
  min-height: 100px;
  padding-top: 2px;
}

.mall-user-card :deep(.van-card__thumb img),
.mall-user-card :deep(.van-card__thumb .van-image) {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: cover;
}

.mall-user-card :deep(.van-card__title) {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 600;
  line-height: 19px;
}

.mall-user-card :deep(.van-card__desc) {
  display: -webkit-box;
  margin-top: 2px;
  overflow: hidden;
  color: var(--text-light);
  text-overflow: ellipsis;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.mall-user-card :deep(.van-card__footer) {
  position: absolute;
  right: 16px;
  bottom: 14px;
  transform: none;
}

/* 标签:仿 MallChat 低饱和标签 */
.mall-user-tag {
  margin-top: 5px;
  margin-right: 6px;
}

.mall-user-card :deep(.van-tag--primary) {
  background: rgba(29, 144, 245, 0.18);
  color: #7cc0ff;
  border-color: rgba(29, 144, 245, 0.35);
  border-radius: 999px;
  font-weight: 400;
}

.mall-user-card :deep(.contact-button--contacted) {
  color: #86efac;
  border-color: rgba(74, 222, 128, 0.38);
  background: rgba(34, 197, 94, 0.14);
}

.mall-user-card :deep(.van-button--primary) {
  background: rgba(29, 144, 245, 0.16);
  color: #7cc0ff;
  border-color: rgba(29, 144, 245, 0.4);
  border-radius: 999px;
}

/* 骨架屏:铺在同样面板上避免闪白 */
.mall-user-card :deep(.van-skeleton__content) {
  padding: 12px;
}
</style>

<template>
  <PartnerCardList>
    <PartnerCard v-for="item in loadingCount" :key="`loading-${item}`" loading />
    <PartnerCard
        v-for="user in props.userList"
        :key="user.id"
        :user="user"
        :contacted="isUserContacted(user.id)"
        :contacting="contactingUserId === user.id"
        @contact="contactUser"
    />
  </PartnerCardList>
</template>

<script setup lang="ts">
import {computed, ref} from 'vue';
import {useRouter} from 'vue-router';
import {Toast} from 'vant';
import myAxios from '../plugins/myAxios';
import type {UserType} from '../models/user';
import {getContactedUserIds, markUserContacted} from '../services/privateChat';
import PartnerCard from './PartnerCard.vue';
import PartnerCardList from './PartnerCardList.vue';

type PartnerCardUser = Omit<UserType, 'tags'> & {tags: string[]};

interface UserCardListProps {
  loading: boolean;
  userList: PartnerCardUser[];
}

const props = withDefaults(defineProps<UserCardListProps>(), {
  loading: true,
  userList: () => [],
});

const router = useRouter();
const contactedUserIds = ref<number[]>(getContactedUserIds());
const contactingUserId = ref<number | null>(null);
const loadingCount = computed(() => props.loading && props.userList.length === 0 ? 4 : 0);
const isUserContacted = (userId: number) => contactedUserIds.value.includes(Number(userId));

const contactUser = async (user: PartnerCardUser) => {
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

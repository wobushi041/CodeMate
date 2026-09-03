<template>
  <div class="mall-page user-page">
    <div class="user-header" v-if="user">
      <van-image
        class="user-avatar"
        round
        width="10rem"
        height="10rem"
        :src="user.avatarUrl || defaultAvatar"
      />
      <div class="user-name">{{ user.username || user.userAccount }}</div>
      <div class="user-tags">
        <span class="user-tags-label">我的标签：</span>
        <template v-if="userTags.length > 0">
          <van-tag
            v-for="tag in userTags"
            :key="tag"
            plain
            type="primary"
            class="user-tag"
          >
            {{ tag }}
          </van-tag>
        </template>
        <span v-else class="user-tags-empty">未设置</span>
      </div>
    </div>
    <div class="user-menu" v-if="user">
      <van-cell class="mall-prof-cell" title="编辑个人信息" is-link to="/user/update" />
      <van-cell class="mall-prof-cell" title="我创建的队伍" is-link to="/user/team/create" />
      <van-cell class="mall-prof-cell" title="我加入的队伍" is-link to="/user/team/join" />
    </div>
    <div class="user-logout" v-if="user">
      <LogoutButton />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {getCurrentUser} from "../services/user";
import LogoutButton from "../components/LogoutButton.vue";


const user = ref();
const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg';

const normalizeTags = (rawTags: unknown): string[] => {
  if (!rawTags) {
    return [];
  }
  if (Array.isArray(rawTags)) {
    return rawTags.map(tag => String(tag).trim()).filter(Boolean);
  }
  const tagsText = String(rawTags).trim();
  if (!tagsText) {
    return [];
  }
  try {
    const parsedTags = JSON.parse(tagsText);
    return Array.isArray(parsedTags)
      ? parsedTags.map(tag => String(tag).trim()).filter(Boolean)
      : [String(parsedTags).trim()].filter(Boolean);
  } catch (error) {
    return tagsText
      .split(/[,，]/)
      .map(tag => tag.trim())
      .filter(Boolean);
  }
}

const userTags = computed(() => normalizeTags(user.value?.tags));

onMounted(async () => {
  user.value = await getCurrentUser();
})
</script>

<style scoped>
.user-page {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 20px 0 4px;
}

.user-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 8px 0 12px;
}

.user-avatar {
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.28);
}

.user-name {
  max-width: calc(100% - 32px);
  color: var(--text-main);
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-tags {
  box-sizing: border-box;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 6px;
  width: 100%;
  max-width: calc(100% - 32px);
  color: var(--text-light);
  font-size: 14px;
  line-height: 22px;
  text-align: center;
}

.user-tags-label {
  color: #d8edff;
}

.user-tag {
  background: rgba(29, 144, 245, 0.14);
  color: #9fd3ff;
  border-color: rgba(29, 144, 245, 0.42);
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(29, 144, 245, 0.12);
}

.user-tags-empty {
  color: var(--text-light);
}

.user-menu {
  flex: 0 0 auto;
}

.user-logout {
  margin-top: auto;
  padding-top: 16px;
}

.user-page :deep(.van-cell) {
  box-sizing: border-box;
  width: calc(100% - 24px);
  border: 1px solid var(--border-weak);
  border-radius: 12px;
  margin: 8px 12px;
}

.user-page :deep(.van-cell__title) {
  color: var(--text-main);
}
</style>

<template>
  <section class="profile-page">
    <header class="profile-header">
      <h1>个人信息</h1>
    </header>

    <main class="profile-content">
      <section v-if="user" class="profile-summary">
        <div class="profile-avatar">
          <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.username || user.userAccount" />
          <div v-else class="profile-avatar__placeholder">
            <ImageIcon :size="48" :stroke-width="1.6" />
            <span>暂时无法查看</span>
          </div>
        </div>

        <h2>{{ user.username || user.userAccount }}</h2>

        <div class="profile-tags">
          <span class="profile-tags__label">我的标签:</span>
          <template v-if="userTags.length > 0">
            <span v-for="tag in userTags" :key="tag" class="profile-tag">{{ tag }}</span>
          </template>
          <span v-else class="profile-tag profile-tag--empty">未设置</span>
        </div>
      </section>

      <section v-if="user" class="profile-menu" aria-label="个人功能">
        <button
            v-for="item in menuItems"
            :key="item.path"
            class="profile-menu__item"
            type="button"
            @click="router.push(item.path)"
        >
          <span>{{ item.label }}</span>
          <ChevronRight :size="20" :stroke-width="1.8" />
        </button>
      </section>

      <div v-if="user" class="profile-logout">
        <LogoutButton />
      </div>
    </main>
  </section>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {ChevronRight, Image as ImageIcon} from 'lucide-vue-next';
import {getCurrentUser} from '../services/user';
import type {UserType} from '../models/user';
import LogoutButton from '../components/LogoutButton.vue';

const router = useRouter();
const user = ref<UserType | null>(null);

const menuItems = [
  {label: '编辑个人信息', path: '/user/update'},
  {label: '我创建的队伍', path: '/user/team/create'},
  {label: '我加入的队伍', path: '/user/team/join'},
];

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
};

const userTags = computed(() => normalizeTags(user.value?.tags));

onMounted(async () => {
  user.value = await getCurrentUser();
});
</script>

<style scoped>
.profile-page {
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: #f8fafc;
  background: #0b1120;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.profile-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  min-height: 84px;
  padding: 22px 16px 12px;
}

.profile-header h1 {
  margin: 0;
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
  line-height: 28px;
  letter-spacing: -0.025em;
}

.profile-content {
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  padding: 28px 16px 48px;
  overflow-y: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.profile-content::-webkit-scrollbar {
  display: none;
}

.profile-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48px;
}

.profile-avatar {
  position: relative;
  width: 128px;
  height: 128px;
  box-sizing: border-box;
  margin-bottom: 24px;
  overflow: hidden;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  background: #1e293b;
  box-shadow: 0 18px 35px rgba(2, 6, 23, 0.45);
}

.profile-avatar::after {
  position: absolute;
  inset: 0;
  content: '';
  pointer-events: none;
  background: linear-gradient(to bottom, transparent 55%, rgba(0, 0, 0, 0.2));
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar__placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #475569;
}

.profile-avatar__placeholder span {
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

.profile-summary h2 {
  max-width: 100%;
  margin: 0 0 12px;
  overflow: hidden;
  color: #f8fafc;
  font-size: 24px;
  font-weight: 800;
  line-height: 32px;
  letter-spacing: -0.025em;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-tags {
  display: flex;
  max-width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #cbd5e1;
  font-size: 14px;
}

.profile-tags__label {
  color: #94a3b8;
  font-weight: 500;
}

.profile-tag {
  padding: 6px 16px;
  border-radius: 999px;
  color: #3b82f6;
  background: #334155;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.profile-tag--empty {
  color: #94a3b8;
}

.profile-menu {
  display: grid;
  gap: 16px;
  margin-bottom: 64px;
}

.profile-menu__item {
  display: flex;
  width: 100%;
  min-height: 64px;
  box-sizing: border-box;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border: 1px solid rgba(51, 65, 85, 0.5);
  border-radius: 16px;
  color: #f8fafc;
  background: #1e293b;
  box-shadow: 0 10px 24px rgba(2, 6, 23, 0.24);
  font: inherit;
  transition: color 0.18s ease, background 0.18s ease, transform 0.15s ease;
}

.profile-menu__item span {
  font-size: 16px;
  font-weight: 600;
}

.profile-menu__item svg {
  color: #94a3b8;
  transition: color 0.18s ease, transform 0.18s ease;
}

.profile-menu__item:active {
  transform: scale(0.985);
  background: rgba(51, 65, 85, 0.72);
}

.profile-menu__item:active svg {
  color: #ffffff;
  transform: translateX(2px);
}

.profile-logout {
  padding: 0 16px;
}

@media (max-width: 359px) {
  .profile-content {
    padding-right: 12px;
    padding-left: 12px;
  }

  .profile-logout {
    padding-right: 4px;
    padding-left: 4px;
  }
}
</style>

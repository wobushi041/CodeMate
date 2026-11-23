<template>
  <view class="prototype-tabbar">
    <button
      v-for="item in navItems"
      :key="item.path"
      class="prototype-tabbar__item"
      :class="{ 'prototype-tabbar__item--active': active === item.name }"
      @click="goTab(item.path)"
    >
      <view class="prototype-tabbar__icon-wrap">
        <wd-icon :name="item.icon" size="24px" />
        <view v-if="item.badge > 0" class="prototype-tabbar__badge">{{ badgeText(item.badge) }}</view>
      </view>
      <text class="prototype-tabbar__label">{{ item.label }}</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useUnreadStore } from '@/store/unread'

defineProps<{ active: 'home' | 'match' | 'team' | 'ai' | 'user' }>()

const unreadStore = useUnreadStore()
let refreshTimer: ReturnType<typeof setInterval> | null = null

const navItems = computed(() => [
  { name: 'home', label: '主页', path: '/pages/index/index', icon: 'home', badge: 0 },
  { name: 'match', label: '匹配', path: '/pages/match/index', icon: 'heart', badge: unreadStore.privateTotal },
  { name: 'team', label: '队伍', path: '/pages/team/index', icon: 'usergroup', badge: unreadStore.teamTotal },
  { name: 'ai', label: 'AI助手', path: '/pages/ai/index', icon: 'chat', badge: 0 },
  { name: 'user', label: '我的', path: '/pages/user/index', icon: 'user', badge: 0 },
] as const)

onMounted(() => {
  void unreadStore.refresh()
  refreshTimer = setInterval(() => { void unreadStore.refresh() }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = null
})

function badgeText(count: number): string {
  return count > 99 ? '99+' : String(count)
}

function goTab(path: string) {
  uni.reLaunch({ url: path })
}
</script>

<style scoped>
.prototype-tabbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: flex;
  height: calc(64px + env(safe-area-inset-bottom));
  align-items: flex-start;
  padding: 0 8px env(safe-area-inset-bottom);
  border-top: 1px solid #ffffff;
  background: rgba(30, 41, 59, .96);
  box-shadow: 0 -10px 26px rgba(2, 6, 23, .18);
  backdrop-filter: blur(16px);
}
.prototype-tabbar__item {
  display: flex;
  height: 64px;
  margin: 0;
  padding: 9px 2px 5px;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border: 0;
  border-radius: 0;
  color: #85817a;
  background: transparent;
  line-height: normal;
}
.prototype-tabbar__item::after { border: 0; }
.prototype-tabbar__item:active { transform: scale(.94); }
.prototype-tabbar__item--active { color: #242321; }
.prototype-tabbar__icon-wrap { position: relative; display: grid; height: 27px; place-items: center; }
.prototype-tabbar__label { font-size: 11px; font-weight: 500; line-height: 16px; white-space: nowrap; }
.prototype-tabbar__badge { position: absolute; top: -5px; right: -12px; display: grid; min-width: 18px; height: 18px; padding: 0 4px; place-items: center; border: 2px solid #ffffff; border-radius: 999px; color: #fff; background: #ad4a46; font-size: 9px; font-weight: 700; line-height: 14px; }
/* 与原 H5 labverse.css 一致的悬浮胶囊导航 */
.prototype-tabbar { right: 16px; bottom: calc(14px + env(safe-area-inset-bottom)); left: 16px; width: auto; height: 60px; align-items: center; padding: 5px 7px; border: 1px solid rgba(255,255,255,.16); border-radius: 999px; background: rgba(39,37,35,.92); box-shadow: 0 15px 30px rgba(39,34,27,.2); }
.prototype-tabbar__item { height: 50px; justify-content: center; gap: 0; padding: 0; color: #c8c3bb; }
.prototype-tabbar__icon-wrap { width: 48px; height: 48px; border-radius: 50%; }
.prototype-tabbar__item--active .prototype-tabbar__icon-wrap { color: #242321; background: #fff; box-shadow: 0 3px 9px rgba(0,0,0,.12); }
.prototype-tabbar__label { display: none; }
.prototype-tabbar__badge { border-color: #292724; background: #d25157; }
</style>

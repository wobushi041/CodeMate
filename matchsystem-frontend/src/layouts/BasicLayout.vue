<template>
  <div class="basic-layout" :class="{ 'basic-layout--with-header': showSharedHeader }">
    <header v-if="showSharedHeader" class="layout-header">
      <button class="layout-header__back" type="button" aria-label="返回" @click="onBack">
        <ArrowLeft :size="24" :stroke-width="2" />
      </button>
      <h1>{{ pageTitle }}</h1>
      <span class="layout-header__spacer" aria-hidden="true" />
    </header>

    <main id="content" class="layout-content">
      <router-view />
    </main>

    <nav class="prototype-tabbar" aria-label="主导航">
      <button
          v-for="item in navItems"
          :key="item.name"
          class="prototype-tabbar__item"
          :class="{ 'prototype-tabbar__item--active': isNavActive(item.path) }"
          type="button"
          @click="goTab(item.path)"
      >
        <span class="prototype-tabbar__icon-wrap">
          <component :is="item.icon" :size="24" :stroke-width="1.8" />
          <span v-if="item.badge" class="prototype-tabbar__badge" />
        </span>
        <span class="prototype-tabbar__label">{{ item.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ArrowLeft, Heart, Home, MessageSquare, UserRound, UsersRound} from 'lucide-vue-next';

const router = useRouter();
const route = useRoute();

const showSharedHeader = computed(() => route.meta.headerMode === 'back');
const pageTitle = computed(() => String(route.meta.title || 'AI编程匹配助手'));

const navItems = [
  {name: 'home', label: '主页', path: '/', icon: Home, match: (path: string) => path === '/'},
  {name: 'match', label: '匹配', path: '/match', icon: Heart, match: (path: string) => path === '/match'},
  {name: 'team', label: '队伍', path: '/team', icon: UsersRound, match: (path: string) => path.startsWith('/team')},
  {name: 'ai', label: 'AI助手', path: '/ai/chat', icon: MessageSquare, badge: true, match: (path: string) => path === '/ai/chat'},
  {name: 'user', label: '我的', path: '/user', icon: UserRound, match: (path: string) => path.startsWith('/user')},
];

const isNavActive = (path: string) => navItems.find(item => item.path === path)?.match(route.path) ?? false;

const goTab = (path: string) => {
  if (path === '/ai/chat') {
    router.push({path, query: {silentGreet: '1'}});
    return;
  }
  router.push(path);
};

const onBack = () => router.back();
</script>

<style scoped>
.basic-layout {
  --app-header-height: 0px;
  --app-tabbar-height: calc(64px + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  flex-direction: column;
  overflow: hidden;
  color: #f8fafc;
  background: #0f172a;
}

.basic-layout--with-header {
  --app-header-height: 56px;
}

.layout-header {
  position: relative;
  z-index: 20;
  display: grid;
  height: var(--app-header-height);
  box-sizing: border-box;
  flex: 0 0 var(--app-header-height);
  grid-template-columns: 48px minmax(0, 1fr) 48px;
  align-items: center;
  padding: 0 8px;
  border-bottom: 1px solid #1e293b;
  color: #f8fafc;
  background: #0f172a;
  box-shadow: 0 4px 14px rgba(2, 6, 23, 0.18);
}

.layout-header h1 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
  letter-spacing: -0.02em;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-header__back {
  display: grid;
  width: 44px;
  height: 44px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: #f8fafc;
  background: transparent;
  transition: background 0.18s ease, transform 0.15s ease;
}

.layout-header__back:active {
  transform: scale(0.92);
  background: rgba(255, 255, 255, 0.08);
}

.layout-header__spacer {
  width: 44px;
  height: 44px;
}

.layout-content {
  box-sizing: border-box;
  width: 100%;
  height: calc(100vh - var(--app-header-height));
  height: calc(100dvh - var(--app-header-height));
  min-height: 0;
  flex: 0 0 auto;
  padding-bottom: var(--app-tabbar-height);
  overflow-x: hidden;
  overflow-y: auto;
  color: #f8fafc;
  background: #0f172a;
  -webkit-overflow-scrolling: touch;
}

.prototype-tabbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: flex;
  height: var(--app-tabbar-height);
  box-sizing: border-box;
  align-items: flex-start;
  padding: 0 8px env(safe-area-inset-bottom);
  border-top: 1px solid #1e293b;
  background: rgba(30, 41, 59, 0.96);
  box-shadow: 0 -10px 26px rgba(2, 6, 23, 0.18);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.prototype-tabbar__item {
  display: flex;
  height: 64px;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 9px 2px 5px;
  border: 0;
  color: #94a3b8;
  background: transparent;
  font: inherit;
  transition: color 0.18s ease, transform 0.15s ease;
}

.prototype-tabbar__item:active {
  transform: scale(0.94);
}

.prototype-tabbar__item--active {
  color: #f8fafc;
}

.prototype-tabbar__icon-wrap {
  position: relative;
  display: grid;
  height: 27px;
  place-items: center;
}

.prototype-tabbar__label {
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
  white-space: nowrap;
}

.prototype-tabbar__badge {
  position: absolute;
  top: -1px;
  right: -4px;
  width: 7px;
  height: 7px;
  border: 2px solid #1e293b;
  border-radius: 50%;
  background: #ef4444;
}
</style>

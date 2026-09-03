<template>
  <div class="basic-layout" :class="{ 'basic-layout--auth': isAuthPage }">
    <van-nav-bar
        v-if="!isAuthPage"
        class="mall-nav"
        :title="title"
        :left-arrow="!isMainTabPage"
        @click-left="onClickLeft"
        @click-right="onClickRight"
    >
      <template v-if="isIndexPage" #right>
        <van-icon name="search" size="18"/>
      </template>
    </van-nav-bar>
    <main id="content" class="mall-content" :class="{ 'mall-content--auth': isAuthPage }">
      <router-view/>
    </main>
    <van-tabbar v-if="!isAuthPage" class="mall-tabbar" route @change="onChange">
      <van-tabbar-item to="/" icon="home-o" name="index">主页</van-tabbar-item>
      <van-tabbar-item to="/team" icon="search" name="team">队伍</van-tabbar-item>
      <van-tabbar-item :to="{ path: '/ai/chat', query: { silentGreet: '1' } }" icon="chat-o" name="ai">AI编程助手</van-tabbar-item>
      <van-tabbar-item to="/user" icon="friends-o" name="user">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import {computed, ref} from "vue";
import routes from "../config/route";

const router = useRouter();
const route = useRoute();
const DEFAULT_TITLE = 'AI编程匹配助手';
const AUTH_PAGE_PATHS = ['/user/login', '/user/register'];
const MAIN_TAB_PAGE_PATHS = ['/', '/team', '/ai/chat', '/user'];
const title = ref(DEFAULT_TITLE);
const isAuthPage = computed(() => AUTH_PAGE_PATHS.includes(route.path));
const isIndexPage = computed(() => route.path === '/');
const isMainTabPage = computed(() => MAIN_TAB_PAGE_PATHS.includes(route.path));

/**
 * 根据路由切换标题
 */
router.beforeEach((to, from) => {
  const toPath = to.path;
  const route = routes.find((route) => {
    return toPath == route.path;
  })
  title.value = route?.title ?? DEFAULT_TITLE;
})

const onClickLeft = () => {
  router.back();
};

const onClickRight = () => {
  router.push('/search')
};

</script>

<style scoped>
.basic-layout {
  --app-nav-height: 46px;
  --app-tabbar-height: 50px;

  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.basic-layout--auth {
  --app-nav-height: 0px;
  --app-tabbar-height: 0px;
}

/* 顶部导航:深色面板 + 底部微弱分隔线 */
.mall-nav {
  flex: 0 0 var(--app-nav-height);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.25);
}

.mall-nav :deep(.van-nav-bar__title) {
  font-weight: 600;
}

.mall-content {
  box-sizing: border-box;
  flex: 0 0 auto;
  height: calc(100vh - var(--app-nav-height) - var(--app-tabbar-height));
  height: calc(100dvh - var(--app-nav-height) - var(--app-tabbar-height));
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  color: var(--text-main);
  -webkit-overflow-scrolling: touch;
}

.mall-content--auth {
  height: 100vh;
  height: 100dvh;
}

/* 底部标签栏:固定最底部 + 深色面板 + 顶部 1px 分隔线 */
.mall-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: var(--app-tabbar-height);
  z-index: 100;
  border-top: 1px solid var(--border-weak);
  box-shadow: 0 -1px 6px rgba(0, 0, 0, 0.18);
}

.mall-tabbar :deep(.van-tabbar-item) {
  color: var(--text-light);
  transition: color 0.3s;
}

.mall-tabbar :deep(.van-tabbar-item--active) {
  color: var(--color-primary);
}
</style>

<template>
  <button class="logout-button" type="button" @click="handleLogout">
    <LogOut :size="20" :stroke-width="1.9" />
    <span>退出登录</span>
  </button>
</template>

<script setup lang="ts">
import {useRouter} from 'vue-router';
import {Toast} from 'vant';
import {LogOut} from 'lucide-vue-next';
import myAxios from '../plugins/myAxios';
import {clearCurrentUserState} from '../states/user';

const router = useRouter();

const handleLogout = async () => {
  try {
    const res = await myAxios.post('/user/logout');
    if (res.code !== 0) {
      Toast.fail(res.description || res.message || '退出登录失败');
      return;
    }
    clearCurrentUserState();
    Toast.success('已退出登录');
    await router.replace('/user/login');
  } catch (error) {
    Toast.fail('退出登录失败');
  }
};
</script>

<style scoped>
.logout-button {
  display: flex;
  width: 100%;
  height: 56px;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 20px;
  border: 1px solid #991b1b;
  border-radius: 999px;
  color: #f87171;
  background: rgba(69, 10, 10, 0.6);
  box-shadow: 0 16px 32px rgba(2, 6, 23, 0.34);
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  transition: background 0.18s ease, transform 0.15s ease;
}

.logout-button:active {
  transform: scale(0.98);
  background: rgba(127, 29, 29, 0.72);
}
</style>

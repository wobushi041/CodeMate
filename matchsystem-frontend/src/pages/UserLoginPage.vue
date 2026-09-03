<template>
  <div class="auth-page flex flex-1 flex-col justify-center bg-gray-900 px-6 py-8 sm:px-8">
    <div class="mx-auto w-full max-w-sm">
      <div class="flex justify-center">
        <img class="h-10 w-auto" :src="logo" alt="编程匹配助手" />
      </div>
      <h2 class="mt-5 text-center text-2xl font-bold tracking-tight text-white">编程匹配助手</h2>
    </div>

    <div class="mx-auto mt-8 w-full max-w-sm">
      <van-form @submit="onSubmit" class="space-y-5">
        <div>
          <label for="userAccount" class="block text-sm font-medium text-gray-100">请输入账号</label>
          <div class="mt-2">
            <input
              type="text"
              id="userAccount"
              name="userAccount"
              autocomplete="username"
              v-model="userAccount"
              required=""
              class="block w-full rounded-md bg-white/5 px-3 py-1.5 text-base text-white outline-1 -outline-offset-1 outline-white/10 placeholder:text-gray-500 focus:outline-2 focus:-outline-offset-2 focus:outline-indigo-500 sm:text-sm"
            />
          </div>
        </div>

        <div>
          <div class="flex items-center justify-between">
            <label for="userPassword" class="block text-sm font-medium text-gray-100">请输入密码</label>
          </div>
          <div class="mt-2">
            <input
              type="password"
              id="userPassword"
              name="userPassword"
              autocomplete="current-password"
              v-model="userPassword"
              required=""
              class="block w-full rounded-md bg-white/5 px-3 py-1.5 text-base text-white outline-1 -outline-offset-1 outline-white/10 placeholder:text-gray-500 focus:outline-2 focus:-outline-offset-2 focus:outline-indigo-500 sm:text-sm"
            />
          </div>
        </div>

        <div>
          <button
            type="submit"
            class="flex w-full justify-center rounded-md bg-indigo-500 px-3 py-1.5 text-sm font-semibold text-white hover:bg-indigo-400 transition-colors duration-150"
          >
            登录
          </button>
        </div>
      </van-form>

      <p class="mt-8 text-center text-sm text-gray-400">
        还没有账号?
        {{ ' ' }}
        <a href="#" class="font-semibold text-indigo-400 hover:text-indigo-300" @click.prevent="toRegister">
          点击这里注册
        </a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import {ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import logo from "../assets/logo.png";

const router = useRouter();
const route = useRoute();

const userAccount = ref('');
const userPassword = ref('');

/**
 * 跳转注册页,并携带当前 redirect 参数,注册成功后可原路返回目标页面
 */
const toRegister = () => {
  router.push({
    path: '/user/register',
    query: route.query?.redirect ? { redirect: route.query.redirect } : {},
  });
};

const onSubmit = async () => {
  const res = await myAxios.post('/user/login', {
    userAccount: userAccount.value,
    userPassword: userPassword.value,
  })
  console.log(res, '用户登录');
  if (res.code === 0 && res.data) {
    Toast.success('登录成功');
    // 跳转到之前的页面
    const redirectUrl = route.query?.redirect as string ?? '/';
    window.location.href = redirectUrl;
  } else {
    Toast.fail('登录失败');
  }
};

</script>

<style scoped>
.auth-page {
  box-sizing: border-box;
  height: 100%;
  min-height: 100%;
}

.auth-page input,
.auth-page button {
  box-sizing: border-box;
}

/* 输入框 autofill 时保持深色背景,避免浏览器灌白 */
input:-webkit-autofill {
  -webkit-box-shadow: 0 0 0 1000px rgba(255, 255, 255, 0.05) inset !important;
  -webkit-text-fill-color: #fff !important;
  transition: background-color 9999s ease-out;
}
</style>

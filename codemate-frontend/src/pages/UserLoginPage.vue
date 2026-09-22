<template>
  <section class="login-page">

    <main class="login-content">
      <section class="login-brand">
        <div class="login-logo-wrap">
          <img :src="logo" alt="编程匹配助手" class="login-logo" />
        </div>
        <p>快速发掘和你默契最合的技术拍档</p>
      </section>

      <section class="login-card">
        <van-form @submit="onSubmit">
          <div class="login-field-group">
            <label for="userAccount">请输入账号</label>
            <input
              id="userAccount"
              v-model="userAccount"
              type="text"
              name="userAccount"
              autocomplete="username"
              placeholder="你的账号"
              required
            />
          </div>

          <div class="login-field-group">
            <label for="userPassword">请输入密码</label>
            <div class="login-password-wrap">
              <input
                id="userPassword"
                v-model="userPassword"
                :type="showPassword ? 'text' : 'password'"
                name="userPassword"
                autocomplete="current-password"
                placeholder="你的密码"
                required
              />
              <button type="button" aria-label="显示或隐藏密码" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="20" :stroke-width="1.8" />
                <Eye v-else :size="20" :stroke-width="1.8" />
              </button>
            </div>
          </div>

          <button class="login-submit" type="submit" :disabled="isLoggingIn">
            <LoaderCircle v-if="isLoggingIn" class="login-spinner" :size="20" :stroke-width="2" />
            <span>{{ isLoggingIn ? '正在登录...' : '登录' }}</span>
          </button>
        </van-form>
      </section>

      <p class="login-register">
        还没有账号？
        <button type="button" @click="toRegister">点击这里注册</button>
      </p>
    </main>
  </section>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {Eye, EyeOff, LoaderCircle} from 'lucide-vue-next';
import {Toast} from 'vant';
import myAxios from '../plugins/myAxios';
import logo from '../assets/brand-logo.png';
import {setCurrentUserState} from '../states/user';

const router = useRouter();
const route = useRoute();
const userAccount = ref('');
const userPassword = ref('');
const showPassword = ref(false);
const isLoggingIn = ref(false);

onMounted(() => {
  // 如果从注册页重定向过来带了已注册账号，自动预填
  if (typeof route.query?.account === 'string') {
    userAccount.value = route.query.account;
  }
});

const toRegister = () => {
  router.push({
    path: '/user/register',
    query: route.query?.redirect ? {redirect: route.query.redirect} : {},
  });
};

/**
 * 校验并提取安全的站内重定向路径，防范开放重定向（Open Redirect）漏洞
 */
const getSafeRedirectUrl = (rawRedirect: unknown): string => {
  if (typeof rawRedirect !== 'string' || !rawRedirect) {
    return '/';
  }
  // 1. 严格校验站内相对路径（以单个 / 开头，排除 // 协议相对地址）
  if (rawRedirect.startsWith('/') && !rawRedirect.startsWith('//')) {
    return rawRedirect.startsWith('/user/login') ? '/' : rawRedirect;
  }
  // 2. 绝对路径仅允许同源地址
  try {
    const url = new URL(rawRedirect, window.location.origin);
    if (url.origin === window.location.origin) {
      const pathWithQuery = url.pathname + url.search + url.hash;
      return pathWithQuery.startsWith('/user/login') ? '/' : pathWithQuery;
    }
  } catch {
    // 忽略异常，走兜底首页
  }
  return '/';
};

const onSubmit = async () => {
  if (isLoggingIn.value) {
    return;
  }
  isLoggingIn.value = true;
  try {
    const res = await myAxios.post('/user/login', {
      userAccount: userAccount.value,
      userPassword: userPassword.value,
    });
    console.log(res, '用户登录');
    if (res.code === 0 && res.data) {
      setCurrentUserState(res.data);
      Toast.success('登录成功');
      const redirectUrl = getSafeRedirectUrl(route.query?.redirect);
      window.location.href = redirectUrl;
    } else {
      Toast.fail('登录失败');
    }
  } finally {
    isLoggingIn.value = false;
  }
};
</script>

<style scoped>
.login-page{position:relative;box-sizing:border-box;width:100%;height:100%;min-height:100%;overflow:hidden;color:#f8fafc;background-color:#070b14;background-image:radial-gradient(circle at 48% 18%,rgba(37,99,235,.28) 0,rgba(37,99,235,.08) 28%,transparent 52%),radial-gradient(circle at 72% 58%,rgba(99,102,241,.22) 0,transparent 38%),radial-gradient(circle at 30% 52%,rgba(14,165,233,.12) 0,transparent 36%);background-repeat:no-repeat;background-size:100% 100%;font-family:Inter,"PingFang SC","Microsoft YaHei",sans-serif}.login-content{position:relative;z-index:1;display:flex;box-sizing:border-box;width:100%;min-height:100%;flex-direction:column;align-items:center;justify-content:center;padding:52px 24px 100px}.login-brand{display:flex;flex-direction:column;align-items:center;margin-bottom:64px;text-align:center}.login-logo-wrap{position:relative;display:grid;width:100%;margin-bottom:14px;place-items:center}.login-logo-wrap::after{position:absolute;z-index:-1;inset:-6px;border-radius:24px;background:rgba(59,130,246,.18);box-shadow:0 0 32px rgba(59,130,246,.45);content:'';filter:blur(16px)}.login-logo{position:relative;z-index:1;display:block;width:190px;height:105px;margin:0 auto;object-fit:contain;filter:drop-shadow(0 10px 18px rgba(2,6,23,.3))}.login-brand h1{margin:0;color:#f8fafc;font-size:30px;font-weight:800;line-height:38px;letter-spacing:-.03em}.login-brand p{margin:8px 0 0;color:#cbd5e1;font-size:14px;line-height:20px}.login-card{box-sizing:border-box;width:88%;max-width:380px;margin:0 auto 40px;padding:0;border:0;background:transparent;box-shadow:none;backdrop-filter:none;-webkit-backdrop-filter:none}.login-field-group{display:grid;gap:12px}.login-field-group+.login-field-group{margin-top:28px}.login-field-group label{padding-left:4px;color:#cbd5e1;font-size:14px;font-weight:500;line-height:20px}.login-field-group input{width:100%;height:56px;box-sizing:border-box;padding:0 24px;border:1px solid rgba(255,255,255,.05);border-radius:16px;outline:none;color:#f8fafc;background:rgba(255,255,255,.02);box-shadow:inset 0 2px 8px rgba(2,6,23,.1);font:inherit;font-size:14px;transition:border-color .18s ease,box-shadow .18s ease}.login-field-group input::placeholder{color:#94a3b8}.login-field-group input:focus{border-color:#fff;box-shadow:0 0 0 1px #fff,inset 0 2px 8px rgba(2,6,23,.1)}.login-password-wrap{position:relative}.login-password-wrap input{padding-right:58px}.login-password-wrap button{position:absolute;top:50%;right:14px;display:grid;width:40px;height:40px;padding:0;transform:translateY(-50%);place-items:center;border:0;border-radius:50%;color:#94a3b8;background:transparent}.login-password-wrap button:active{color:#f8fafc;background:rgba(255,255,255,.08)}.login-submit{display:flex;width:100%;height:56px;align-items:center;justify-content:center;gap:10px;margin-top:28px;border:0;border-radius:999px;color:#030712;background:#fff;box-shadow:0 12px 24px rgba(2,6,23,.28);font:inherit;font-size:14px;font-weight:600;transition:background .18s ease,transform .15s ease,opacity .18s ease}.login-submit:active{transform:scale(.98)}.login-submit:hover{background:#f4f4f5}.login-submit:disabled{cursor:not-allowed;opacity:.7}.login-spinner{animation:login-spin 1s linear infinite}.login-register{position:relative;z-index:1;margin:0;color:#cbd5e1;font-size:14px;line-height:20px;text-align:center}.login-register button{padding:0;border:0;color:#3b82f6;background:transparent;font:inherit;font-weight:600}.login-register button:active{color:#60a5fa}@keyframes login-spin{to{transform:rotate(360deg)}}@media(max-width:420px){.login-content{padding-right:24px;padding-left:24px}}@media(max-height:720px){.login-content{justify-content:flex-start;padding-top:88px;padding-bottom:80px;overflow-y:auto}.login-brand{margin-bottom:40px}.login-logo{width:160px;height:89px}.login-brand h1{font-size:26px;line-height:34px}}

input:-webkit-autofill{-webkit-box-shadow:0 0 0 1000px rgba(255,255,255,.02) inset !important;-webkit-text-fill-color:#fff !important;transition:background-color 9999s ease-out}
</style>

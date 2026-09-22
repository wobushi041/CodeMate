<template>
  <section class="register-page">

    <main class="register-content">
      <section class="register-brand">
        <div class="register-logo-wrap">
          <img :src="logo" alt="编程匹配助手" class="register-logo" />
        </div>
        <h2>注册新账号</h2>
      </section>

      <section class="register-card">
        <van-form @submit="onSubmit">
          <div class="register-field-group">
            <label for="userAccount">账号</label>
            <input id="userAccount" v-model="userAccount" type="text" name="userAccount" autocomplete="username" placeholder="设置你的账号" required />
          </div>

          <div class="register-field-group">
            <label for="userPassword">密码</label>
            <div class="register-password-wrap">
              <input id="userPassword" v-model="userPassword" :type="showPassword ? 'text' : 'password'" name="userPassword" autocomplete="new-password" placeholder="设置登录密码" required />
              <button type="button" aria-label="显示或隐藏密码" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="20" :stroke-width="1.8" />
                <Eye v-else :size="20" :stroke-width="1.8" />
              </button>
            </div>
          </div>

          <div class="register-field-group">
            <label for="checkPassword">确认密码</label>
            <div class="register-password-wrap">
              <input id="checkPassword" v-model="checkPassword" :type="showCheckPassword ? 'text' : 'password'" name="checkPassword" autocomplete="new-password" placeholder="再次输入登录密码" required />
              <button type="button" aria-label="显示或隐藏确认密码" @click="showCheckPassword = !showCheckPassword">
                <EyeOff v-if="showCheckPassword" :size="20" :stroke-width="1.8" />
                <Eye v-else :size="20" :stroke-width="1.8" />
              </button>
            </div>
          </div>

          <div class="register-field-group">
            <label for="planetCode">用户编号</label>
            <input id="planetCode" v-model="planetCode" type="text" name="planetCode" autocomplete="off" placeholder="设置你的用户编号" required />
          </div>

          <button class="register-submit" type="submit" :disabled="isRegistering">
            <LoaderCircle v-if="isRegistering" class="register-spinner" :size="20" :stroke-width="2" />
            <span>{{ isRegistering ? '正在注册...' : '注册' }}</span>
          </button>
        </van-form>
      </section>

      <p class="register-login">
        已有账号？
        <button type="button" @click="toLogin">点这里登录</button>
      </p>
    </main>
  </section>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {Eye, EyeOff, LoaderCircle} from 'lucide-vue-next';
import {Toast} from 'vant';
import myAxios from '../plugins/myAxios';
import logo from '../assets/brand-logo.png';

const router = useRouter();
const route = useRoute();
const userAccount = ref('');
const userPassword = ref('');
const checkPassword = ref('');
const planetCode = ref('');
const showPassword = ref(false);
const showCheckPassword = ref(false);
const isRegistering = ref(false);

const toLogin = () => {
  router.push({
    path: '/user/login',
    query: route.query?.redirect ? {redirect: route.query.redirect} : {},
  });
};

const checkPasswordIsSame = (val: string) => val === userPassword.value;

const onSubmit = async () => {
  if (isRegistering.value) {
    return;
  }
  if (!checkPasswordIsSame(checkPassword.value)) {
    Toast.fail('两次输入的密码不一致');
    return;
  }
  isRegistering.value = true;
  try {
    const res = await myAxios.post('/user/register', {
      userAccount: userAccount.value,
      userPassword: userPassword.value,
      checkPassword: checkPassword.value,
      planetCode: planetCode.value,
    });
    console.log(res, '用户注册');
    if (res.code === 0 && res.data) {
      Toast.success('注册成功，请登录');
      const redirectParam = typeof route.query?.redirect === 'string' ? route.query.redirect : '';
      router.replace({
        path: '/user/login',
        query: {
          ...(redirectParam ? {redirect: redirectParam} : {}),
          account: userAccount.value,
        },
      });
    } else {
      Toast.fail('注册失败:' + (res.description ?? ''));
    }
  } finally {
    isRegistering.value = false;
  }
};
</script>

<style scoped>
.register-page{position:relative;box-sizing:border-box;width:100%;height:100%;min-height:100%;overflow-x:hidden;overflow-y:auto;-webkit-overflow-scrolling:touch;color:#f8fafc;background-color:#070b14;background-image:radial-gradient(circle at 48% 18%,rgba(37,99,235,.28) 0,rgba(37,99,235,.08) 28%,transparent 52%),radial-gradient(circle at 72% 58%,rgba(99,102,241,.22) 0,transparent 38%),radial-gradient(circle at 30% 52%,rgba(14,165,233,.12) 0,transparent 36%);background-repeat:no-repeat;background-size:100% 100%;font-family:Inter,"PingFang SC","Microsoft YaHei",sans-serif}.register-content{position:relative;z-index:1;display:flex;box-sizing:border-box;width:100%;min-height:100%;flex-direction:column;align-items:center;justify-content:center;padding:36px 24px 44px}.register-brand{display:flex;flex-direction:column;align-items:center;margin-bottom:28px;text-align:center}.register-logo-wrap{position:relative;display:grid;width:100%;margin-bottom:14px;place-items:center}.register-logo-wrap::after{position:absolute;z-index:-1;inset:-6px;border-radius:24px;background:rgba(59,130,246,.18);box-shadow:0 0 32px rgba(59,130,246,.45);content:'';filter:blur(16px)}.register-logo{position:relative;z-index:1;display:block;width:190px;height:105px;margin:0 auto;object-fit:contain;filter:drop-shadow(0 10px 18px rgba(2,6,23,.3))}.register-brand h1{margin:0;color:#f8fafc;font-size:28px;font-weight:800;line-height:36px;letter-spacing:-.03em}.register-brand p{margin:8px 0 0;color:#cbd5e1;font-size:14px;line-height:20px}.register-card{box-sizing:border-box;width:88%;max-width:380px;margin:0 auto 24px;padding:0;border:0;background:transparent;box-shadow:none;backdrop-filter:none;-webkit-backdrop-filter:none}.register-field-group{display:grid;gap:10px}.register-field-group+.register-field-group{margin-top:16px}.register-field-group label{padding-left:4px;color:#cbd5e1;font-size:14px;font-weight:500;line-height:20px}.register-field-group input{width:100%;height:52px;box-sizing:border-box;padding:0 20px;border:1px solid rgba(255,255,255,.05);border-radius:15px;outline:none;color:#f8fafc;background:rgba(255,255,255,.02);box-shadow:inset 0 2px 8px rgba(2,6,23,.1);font:inherit;font-size:14px;transition:border-color .18s ease,box-shadow .18s ease}.register-field-group input::placeholder{color:#94a3b8}.register-field-group input:focus{border-color:#fff;box-shadow:0 0 0 1px #fff,inset 0 2px 8px rgba(2,6,23,.1)}.register-password-wrap{position:relative}.register-password-wrap input{padding-right:58px}.register-password-wrap button{position:absolute;top:50%;right:12px;display:grid;width:40px;height:40px;padding:0;transform:translateY(-50%);place-items:center;border:0;border-radius:50%;color:#94a3b8;background:transparent}.register-password-wrap button:active{color:#f8fafc;background:rgba(255,255,255,.08)}.register-submit{display:flex;width:100%;height:54px;align-items:center;justify-content:center;gap:10px;margin-top:26px;border:0;border-radius:999px;color:#030712;background:#fff;box-shadow:0 12px 24px rgba(2,6,23,.28);font:inherit;font-size:14px;font-weight:600;transition:background .18s ease,transform .15s ease,opacity .18s ease}.register-submit:active{transform:scale(.98)}.register-submit:hover{background:#f4f4f5}.register-submit:disabled{cursor:not-allowed;opacity:.7}.register-spinner{animation:register-spin 1s linear infinite}.register-login{position:relative;z-index:1;margin:0;color:#cbd5e1;font-size:14px;line-height:20px;text-align:center}.register-login button{padding:0;border:0;color:#3b82f6;background:transparent;font:inherit;font-weight:600;cursor:pointer}.register-login button:active{color:#60a5fa}@keyframes register-spin{to{transform:rotate(360deg)}}@media(max-width:420px){.register-content{padding-right:20px;padding-left:20px}}@media(max-height:760px){.register-content{justify-content:flex-start;padding-top:28px;padding-bottom:36px;overflow-y:auto}.register-brand{margin-bottom:20px}.register-logo{width:160px;height:89px}.register-brand h1{font-size:24px;line-height:30px}.register-field-group+.register-field-group{margin-top:14px}}

input:-webkit-autofill{-webkit-box-shadow:0 0 0 1000px rgba(255,255,255,.02) inset !important;-webkit-text-fill-color:#fff !important;transition:background-color 9999s ease-out}
</style>

<template>
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="userAccount"
          name="userAccount"
          label="账号"
          placeholder="请输入账号(至少4位)"
          :rules="[{ required: true, message: '请填写账号' }, { pattern: /^\S{4,}$/, message: '账号不能少于4位' }]"
      />
      <van-field
          v-model="userPassword"
          type="password"
          name="userPassword"
          label="密码"
          placeholder="请输入密码(至少8位)"
          :rules="[{ required: true, message: '请填写密码' }, { pattern: /^\S{8,}$/, message: '密码不能少于8位' }]"
      />
      <van-field
          v-model="checkPassword"
          type="password"
          name="checkPassword"
          label="确认密码"
          placeholder="请再次输入密码"
          :rules="[
            { required: true, message: '请再次输入密码' },
            { validator: checkPasswordIsSame, message: '两次输入的密码不一致' }
          ]"
      />
      <van-field
          v-model="planetCode"
          name="planetCode"
          label="星球编号"
          placeholder="请输入星球编号(不超过5位)"
          :rules="[{ required: true, message: '请填写星球编号' }, { max: 5, message: '星球编号不能超过5位' }]"
      />
    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        注册
      </van-button>
    </div>
  </van-form>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import {ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";

const router = useRouter();
const route = useRoute();

const userAccount = ref('');
const userPassword = ref('');
const checkPassword = ref('');
const planetCode = ref('');

// 校验两次输入的密码是否一致
const checkPasswordIsSame = (val: string) => {
  return val === userPassword.value;
};

const onSubmit = async () => {
  const res = await myAxios.post('/user/register', {
    userAccount: userAccount.value,
    userPassword: userPassword.value,
    checkPassword: checkPassword.value,
    planetCode: planetCode.value,
  })
  console.log(res, '用户注册');
  if (res.code === 0 && res.data) {
    Toast.success('注册成功');
    // 注册成功后直接进入目标页面,有 redirect 则去该页,否则回首页
    const redirectUrl = route.query?.redirect as string ?? '/';
    router.replace(redirectUrl);
  } else {
    Toast.fail(`注册失败:${res.description ?? ''}`);
  }
};
</script>

<style scoped>

</style>
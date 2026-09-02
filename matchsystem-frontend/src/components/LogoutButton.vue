<template>
  <div class="logout-button-wrap">
    <van-button
      block
      plain
      hairline
      type="danger"
      icon="revoke"
      @click="handleLogout"
    >
      退出登录
    </van-button>
  </div>
</template>

<script setup lang="ts">
import {useRouter} from "vue-router";
import {Toast} from "vant";
import myAxios from "../plugins/myAxios";
import {clearCurrentUserState} from "../states/user";

const router = useRouter();

const handleLogout = async () => {
  try {
    const res = await myAxios.post("/user/logout");
    if (res.code !== 0) {
      Toast.fail(res.description || res.message || "退出登录失败");
      return;
    }
    clearCurrentUserState();
    Toast.success("已退出登录");
    await router.replace("/user/login");
  } catch (error) {
    Toast.fail("退出登录失败");
  }
};
</script>

<style scoped>
.logout-button-wrap {
  margin: 12px 0 16px;
}
</style>

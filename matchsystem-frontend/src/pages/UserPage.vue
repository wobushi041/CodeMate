<template>
  <div class="user-page">
    <LogoutButton v-if="user" />
  </div>
  <template v-if="user">
    <van-cell title="当前用户" :value="user?.username" />
    <van-cell title="AI 编程助手" is-link to="/ai/chat" />
    <van-cell title="修改信息" is-link to="/user/update" />
    <van-cell title="我创建的队伍" is-link to="/user/team/create" />
    <van-cell title="我加入的队伍" is-link to="/user/team/join" />
  </template>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import {getCurrentUser} from "../services/user";
import LogoutButton from "../components/LogoutButton.vue";


const user = ref();

onMounted(async () => {
  user.value = await getCurrentUser();
})
</script>

<style scoped>
.user-page {
  padding: 12px 16px 0;
}

</style>

<template>
  <div id="teamCardList" class="team-card-list">
    <article v-for="team in props.teamList" :key="team.id" class="team-card">
      <header class="team-card__header">
        <div class="team-card__heading">
          <h3>{{ team.name }}</h3>
          <p>{{ team.description || '暂无队伍介绍' }}</p>
        </div>
        <span class="team-status" :class="{ 'team-status--locked': team.status === 2 }">
          {{ teamStatusEnum[team.status] }}
        </span>
      </header>

      <section class="team-card__details">
        <div class="team-detail-row">
          <Users :size="16" :stroke-width="1.8" />
          <span>队伍人数: <strong>{{ getTeamMemberCount(team) }}/{{ team.maxNum }}</strong></span>
        </div>
        <div class="team-detail-row">
          <CalendarClock :size="16" :stroke-width="1.8" />
          <span>过期时间: <time>{{ formatTeamTime(team.expireTime) }}</time></span>
        </div>
        <div class="team-detail-row">
          <CalendarDays :size="16" :stroke-width="1.8" />
          <span>创建时间: <time>{{ formatTeamTime(team.createTime) }}</time></span>
        </div>
      </section>

      <footer class="team-actions">
        <button
          v-if="team.userId !== currentUser?.id && !team.hasJoin"
          class="team-action team-action--secondary"
          type="button"
          @click="preJoinTeam(team)"
        >
          <LogIn :size="16" :stroke-width="1.9" />
          <span>加入队伍</span>
        </button>
        <button
          v-if="team.userId === currentUser?.id"
          class="team-action team-action--secondary"
          type="button"
          @click="doUpdateTeam(team.id)"
        >
          <RefreshCw :size="16" :stroke-width="1.9" />
          <span>更新队伍</span>
        </button>
        <button
          v-if="team.userId === currentUser?.id || team.hasJoin"
          class="team-action team-action--primary"
          type="button"
          @click="goTeamChat(team)"
        >
          <MessageCircle :size="16" :stroke-width="1.9" />
          <span>聊天室</span>
        </button>
        <button
          v-if="team.userId !== currentUser?.id && team.hasJoin"
          class="team-action team-action--secondary"
          type="button"
          @click="doQuitTeam(team.id)"
        >
          <LogOut :size="16" :stroke-width="1.9" />
          <span>退出队伍</span>
        </button>
        <button
          v-if="team.userId === currentUser?.id"
          class="team-action team-action--secondary"
          type="button"
          @click="preDeleteTeam(team.id)"
        >
          <UserX :size="16" :stroke-width="1.9" />
          <span>解散队伍</span>
        </button>
      </footer>
    </article>

    <van-popup v-model:show="showPasswordDialog" round position="bottom" class="team-password-popup" @closed="doJoinCancel">
      <div class="team-password-sheet">
        <div class="team-password-title">加入加密队伍</div>
        <div class="team-password-subtitle">请输入队伍密码后继续</div>
        <van-form class="team-password-form" @submit="doJoinTeam">
          <van-field v-model="password" clearable left-icon="lock" name="password" type="password" placeholder="请输入密码" autocomplete="current-password" :rules="[{ required: true, message: '请输入密码' }]" />
          <div class="team-password-actions">
            <van-button block round native-type="button" @click="doJoinCancel">取消</van-button>
            <van-button block round type="primary" native-type="submit" :loading="joining">加入队伍</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showDeleteConfirm" round position="bottom" class="team-password-popup" @closed="doDeleteCancel">
      <div class="team-password-sheet">
        <div class="team-password-title">确认解散队伍</div>
        <div class="team-password-subtitle">解散后队伍将无法恢复，是否继续？</div>
        <div class="team-password-actions team-delete-actions">
          <van-button block round native-type="button" @click="doDeleteCancel">取消</van-button>
          <van-button block round type="danger" native-type="button" :loading="deleting" @click="doDeleteTeam">确认解散</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>
<script setup lang="ts">
import {TeamType} from "../models/team";
import {teamStatusEnum} from "../constants/team";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import {onMounted, ref} from "vue";
import {getCurrentUser} from "../services/user";
import {useRouter} from "vue-router";
import { CalendarClock, CalendarDays, LogIn, LogOut, MessageCircle, RefreshCw, Users, UserX } from "lucide-vue-next";

interface TeamCardListProps {
  teamList: TeamType[];
}

const props = withDefaults(defineProps<TeamCardListProps>(), {
  // @ts-ignore
  teamList: [] as TeamType[],
});

const emit = defineEmits<{
  (e: 'refresh'): void
}>();

const showPasswordDialog = ref(false);
const password = ref('');
const joinTeamId = ref(0);
const currentUser = ref();
const joining = ref(false);
const showDeleteConfirm = ref(false);
const deleteTeamId = ref(0);
const deleting = ref(false);

const router = useRouter();
const formatTeamTime = (value?: Date | string) => {
  if (!value) {
    return '长期有效';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const pad = (number: number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

onMounted(async () => {
  currentUser.value = await getCurrentUser();
})

const getTeamMemberCount = (team: TeamType) => {
  if (typeof team.hasJoinNum === 'number') {
    return team.hasJoinNum;
  }
  if (team.userId === currentUser.value?.id || team.hasJoin) {
    return 1;
  }
  return 0;
}

const preJoinTeam = (team: TeamType) => {
  joinTeamId.value = team.id;
  if (team.status === 0) {
    doJoinTeam()
  } else {
    showPasswordDialog.value = true;
  }
}

const doJoinCancel = () => {
  showPasswordDialog.value = false;
  joinTeamId.value = 0;
  password.value = '';
}

const preDeleteTeam = (id: number) => {
  deleteTeamId.value = id;
  showDeleteConfirm.value = true;
}

const doDeleteCancel = () => {
  if (deleting.value) {
    return;
  }
  showDeleteConfirm.value = false;
  deleteTeamId.value = 0;
}

/**
 * 加入队伍
 */
const doJoinTeam = async () => {
  if (!joinTeamId.value || joining.value) {
    return;
  }
  joining.value = true;
  try {
    const res = await myAxios.post('/team/join', {
      teamId: joinTeamId.value,
      password: password.value
    });
    if (res?.code === 0) {
      Toast.success('加入成功');
      doJoinCancel();
      emit('refresh');
    } else {
      Toast.fail('加入失败' + (res.description ? `，${res.description}` : ''));
    }
  } catch (e) {
    Toast.fail('加入失败');
  } finally {
    joining.value = false;
  }
}

/**
 * 跳转至更新队伍页
 * @param id
 */
const doUpdateTeam = (id: number) => {
  router.push({
    path: '/team/update',
    query: {
      id,
    }
  })
}

const goTeamChat = (team: TeamType) => {
  router.push({
    path: '/team/chat',
    query: {
      teamId: team.id,
      teamName: team.name,
    }
  })
}

/**
 * 退出队伍
 * @param id
 */
const doQuitTeam = async (id: number) => {
  const res = await myAxios.post('/team/quit', {
    teamId: id
  });
  if (res?.code === 0) {
    Toast.success('操作成功');
    emit('refresh');
  } else {
    Toast.fail('操作失败' + (res.description ? `，${res.description}` : ''));
  }
}

/**
 * 解散队伍
 */
const doDeleteTeam = async () => {
  if (!deleteTeamId.value || deleting.value) {
    return;
  }
  const id = deleteTeamId.value;
  deleting.value = true;
  try {
    const res = await myAxios.post('/team/delete', id, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
    if (res?.code === 0) {
      Toast.success('操作成功');
      showDeleteConfirm.value = false;
      deleteTeamId.value = 0;
      emit('refresh');
    } else {
      Toast.fail('操作失败' + (res.description ? `，${res.description}` : ''));
    }
  } catch (e) {
    Toast.fail('操作失败');
  } finally {
    deleting.value = false;
  }
}
</script>

<style scoped>
.team-card-list { box-sizing: border-box; display: grid; gap: 14px; padding: 0 12px 18px; }
.team-card { box-sizing: border-box; padding: 17px 15px 16px; overflow: hidden; border: 1px solid rgba(100,116,139,.32); border-radius: 18px; background: #1e293b; box-shadow: 0 8px 20px rgba(2,6,23,.08); }
.team-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; padding-bottom: 14px; }
.team-card__heading { min-width: 0; flex: 1; }
.team-card__heading h3 { margin: 0; overflow: hidden; color: #f8fafc; font-size: 17px; font-weight: 700; line-height: 22px; text-overflow: ellipsis; white-space: nowrap; }
.team-card__heading p { margin: 4px 0 0; overflow: hidden; color: #cbd5e1; font-size: 12px; line-height: 18px; text-overflow: ellipsis; white-space: nowrap; }
.team-status { flex: 0 0 auto; padding: 4px 10px; border-radius: 999px; color: #60a5fa; background: rgba(59,130,246,.12); font-size: 11px; font-weight: 500; line-height: 18px; white-space: nowrap; }
.team-status--locked { color: #cbd5e1; background: #334155; }
.team-card__details { display: grid; gap: 9px; padding: 13px 0 15px; border-top: 1px solid rgba(100,116,139,.25); color: #cbd5e1; }
.team-detail-row { display: flex; align-items: center; gap: 8px; min-width: 0; font-size: 12px; line-height: 18px; }
.team-detail-row svg { flex: 0 0 auto; color: #94a3b8; }
.team-detail-row strong { color: #f8fafc; font-weight: 600; }
.team-detail-row time { color: #f8fafc; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 10px; }
.team-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; padding-top: 13px; border-top: 1px solid rgba(100,116,139,.25); }
.team-action { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 32px; padding: 7px 13px; border: 0; border-radius: 999px; font: inherit; font-size: 12px; font-weight: 500; line-height: 18px; white-space: nowrap; box-shadow: 0 2px 5px rgba(2,6,23,.12); transition: transform .15s ease, background-color .15s ease; }
.team-action:active { transform: scale(.95); }
.team-action--primary { color: #030712; background: #fff; }
.team-action--secondary { color: #f8fafc; background: #334155; }
.team-password-popup { overflow: hidden; background: #1e293b; }
.team-password-sheet { box-sizing: border-box; padding: 20px 16px calc(18px + env(safe-area-inset-bottom)); background: #1e293b; }
.team-password-title { color: #f8fafc; font-size: 18px; font-weight: 600; line-height: 26px; text-align: center; }
.team-password-subtitle { margin-top: 4px; color: #94a3b8; font-size: 13px; line-height: 20px; text-align: center; }
.team-password-form { margin-top: 18px; }
.team-password-form :deep(.van-cell) { box-sizing: border-box; padding: 12px 16px; border: 1px solid rgba(148,163,184,.18); border-radius: 12px; background: #0f172a; }
.team-password-form :deep(.van-field__left-icon) { color: #94a3b8; }
.team-password-form :deep(.van-field__control) { color: #f8fafc; }
.team-password-actions { display: grid; grid-template-columns: minmax(0,1fr) minmax(0,1fr); gap: 12px; margin-top: 18px; }
.team-password-actions :deep(.van-button) { height: 44px; margin-left: 0; }
.team-delete-actions :deep(.van-button--danger) { color: #fff; background: var(--color-danger); border-color: var(--color-danger); }
</style>


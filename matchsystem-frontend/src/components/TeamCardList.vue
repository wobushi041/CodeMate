<template>
  <div id="teamCardList">
    <van-card
        class="mall-card"
        v-for="team in props.teamList"
        :key="team.id"
        :desc="team.description"
        :title="`${team.name}`"
    >
      <template #tags>
        <van-tag class="team-status-tag" plain type="primary">
          {{
            teamStatusEnum[team.status]
          }}
        </van-tag>
      </template>
      <template #bottom>
        <div class="team-meta">
          {{ `队伍人数: ${getTeamMemberCount(team)}/${team.maxNum}` }}
        </div>
        <div class="team-meta" v-if="team.expireTime">
          {{ '过期时间: ' + team.expireTime }}
        </div>
        <div class="team-meta">
          {{ '创建时间: ' + team.createTime }}
        </div>
      </template>
      <template #footer>
        <div class="team-actions">
          <van-button size="small" type="primary" v-if="team.userId !== currentUser?.id && !team.hasJoin" plain
                      @click="preJoinTeam(team)">
            加入队伍
          </van-button>
          <van-button v-if="team.userId === currentUser?.id" size="small" plain
                      @click="doUpdateTeam(team.id)">更新队伍
          </van-button>
          <van-button v-if="team.userId === currentUser?.id || team.hasJoin" size="small" type="primary" plain
                      @click="goTeamChat(team)">聊天室
          </van-button>
          <van-button v-if="team.userId !== currentUser?.id && team.hasJoin" size="small" plain
                      @click="doQuitTeam(team.id)">退出队伍
          </van-button>
          <van-button v-if="team.userId === currentUser?.id" size="small" plain
                      @click="preDeleteTeam(team.id)">解散队伍
          </van-button>
        </div>
      </template>
    </van-card>
    <van-popup v-model:show="showPasswordDialog" round position="bottom" class="team-password-popup" @closed="doJoinCancel">
      <div class="team-password-sheet">
        <div class="team-password-title">加入加密队伍</div>
        <div class="team-password-subtitle">请输入队伍密码后继续</div>
        <van-form class="team-password-form" @submit="doJoinTeam">
          <van-field
              v-model="password"
              clearable
              left-icon="lock"
              name="password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              :rules="[{ required: true, message: '请输入密码' }]"
          />
          <div class="team-password-actions">
            <van-button block round native-type="button" @click="doJoinCancel">取消</van-button>
            <van-button block round type="primary" native-type="submit" :loading="joining">
              加入队伍
            </van-button>
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
          <van-button block round type="danger" native-type="button" :loading="deleting" @click="doDeleteTeam">
            确认解散
          </van-button>
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
#teamCardList {
  box-sizing: border-box;
  padding: 12px;
}

#teamCardList :deep(.van-card) {
  box-sizing: border-box;
  margin: 0 0 12px;
  padding: 14px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-weak);
  border-radius: 12px;
}

#teamCardList :deep(.van-card:last-child) {
  margin-bottom: 0;
}

#teamCardList :deep(.van-card__content) {
  min-height: 0;
}

#teamCardList :deep(.van-card__title) {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 600;
}

#teamCardList :deep(.van-card__desc) {
  display: -webkit-box;
  margin-top: 8px;
  overflow: hidden;
  color: var(--text-light);
  font-size: 13px;
  line-height: 19px;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.team-status-tag {
  margin-top: 8px;
  margin-right: 8px;
}

#teamCardList :deep(.van-tag--primary) {
  background: rgba(29, 144, 245, 0.18);
  color: #7cc0ff;
  border-color: rgba(29, 144, 245, 0.35);
  border-radius: 6px;
  font-weight: 400;
}

.team-meta {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: var(--text-light);
}

#teamCardList :deep(.van-card__bottom) {
  margin-top: 10px;
}

#teamCardList :deep(.van-card__footer) {
  margin-top: 12px;
}

.team-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.team-actions :deep(.van-button) {
  margin-left: 0;
}

.team-password-popup {
  overflow: hidden;
  background: var(--bg-panel);
}

.team-password-sheet {
  box-sizing: border-box;
  padding: 20px 16px calc(18px + env(safe-area-inset-bottom));
  background: var(--bg-panel);
}

.team-password-title {
  color: var(--text-main);
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
  text-align: center;
}

.team-password-subtitle {
  margin-top: 4px;
  color: var(--text-light);
  font-size: 13px;
  line-height: 20px;
  text-align: center;
}

.team-password-form {
  margin-top: 18px;
}

.team-password-form :deep(.van-cell) {
  box-sizing: border-box;
  padding: 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-weak);
  border-radius: 10px;
}

.team-password-form :deep(.van-field__left-icon) {
  color: var(--text-light);
}

.team-password-form :deep(.van-field__control) {
  color: var(--text-main);
}

.team-password-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  margin-top: 18px;
}

.team-password-actions :deep(.van-button) {
  box-sizing: border-box;
  height: 44px;
  margin-left: 0;
  border-radius: 999px;
}

#teamCardList :deep(.van-button--primary) {
  color: #7cc0ff;
  background: rgba(29, 144, 245, 0.16);
  border-color: rgba(29, 144, 245, 0.4);
  border-radius: 8px;
}

#teamCardList :deep(.van-button--default) {
  color: var(--text-light-1);
  border-color: var(--border-strong);
  border-radius: 8px;
}

.team-password-actions :deep(.van-button--default) {
  color: var(--text-light-1);
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--border-strong);
}

.team-password-actions :deep(.van-button--primary) {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.team-delete-actions :deep(.van-button--danger) {
  color: #fff;
  background: var(--color-danger);
  border-color: var(--color-danger);
  border-radius: 8px;
}
</style>

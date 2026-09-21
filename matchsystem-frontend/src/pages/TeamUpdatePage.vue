<template>
  <div class="team-form-page">
    <header class="form-header">
      <button type="button" aria-label="返回" @click="router.back()"><ChevronLeft :size="28" /></button>
      <h1>更新队伍</h1>
      <span />
    </header>

    <form class="team-form" :class="{ 'team-form--loading': loading }" @submit.prevent="onSubmit">
      <div class="form-card">
        <div class="form-row">
          <label for="updateTeamName">队伍名</label>
          <input id="updateTeamName" v-model.trim="addTeamData.name" class="form-control" type="text" />
        </div>
        <div class="form-row form-row--textarea">
          <label for="updateTeamDescription">队伍描述</label>
          <textarea id="updateTeamDescription" v-model.trim="addTeamData.description" class="form-control" rows="4" />
        </div>
        <div class="form-row">
          <label>过期时间</label>
          <button class="form-control date-control" type="button" :disabled="loading" @click="showPicker = true">
            <span :class="{ placeholder: !expireTimeText }">{{ expireTimeText || '点击选择过期时间' }}</span>
            <ChevronRight :size="20" />
          </button>
        </div>
        <div class="form-row">
          <label>队伍状态</label>
          <div class="status-options">
            <label v-for="option in statusOptions" :key="option.value" class="radio-option">
              <input v-model="addTeamData.status" type="radio" name="updateTeamStatus" :value="option.value" :disabled="loading" />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>
        <div v-if="Number(addTeamData.status) === 2" class="form-row">
          <label for="updateTeamPassword">密码</label>
          <input id="updateTeamPassword" v-model="addTeamData.password" class="form-control" type="password" placeholder="请输入队伍密码" />
        </div>
      </div>

      <div class="fixed-submit">
        <button type="submit" :disabled="submitting || loading">
          <LoaderCircle v-if="submitting || loading" class="loading-icon" :size="20" />
          <span>{{ loading ? '正在加载...' : submitting ? '正在保存...' : '保存修改' }}</span>
        </button>
      </div>
    </form>

    <van-popup v-model:show="showPicker" round position="bottom" class="team-picker-popup">
      <van-datetime-picker v-model="addTeamData.expireTime" type="datetime" title="请选择过期时间" :min-date="minDate" @confirm="onExpireTimeConfirm" @cancel="showPicker = false" />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ChevronLeft, ChevronRight, LoaderCircle } from 'lucide-vue-next';
import { Toast } from 'vant';
import myAxios from '../plugins/myAxios';

type TeamUpdateForm = { id: number; name: string; description: string; expireTime: Date | null; password: string; status: string; };
const router = useRouter();
const route = useRoute();
const showPicker = ref(false);
const loading = ref(false);
const submitting = ref(false);
const minDate = new Date();
const id = Number(route.query.id);
const statusOptions = [{ label: '公开', value: '0' }, { label: '私有', value: '1' }, { label: '加密', value: '2' }];
const initFormData: TeamUpdateForm = { id, name: '', description: '', expireTime: null, password: '', status: '0' };
const addTeamData = ref<TeamUpdateForm>({ ...initFormData });
const expireTimeText = computed(() => addTeamData.value.expireTime ? formatDateTime(addTeamData.value.expireTime) : '');
const parseExpireTime = (value: unknown) => { if (!value) return null; const date = value instanceof Date ? value : new Date(String(value)); return Number.isNaN(date.getTime()) ? null : date; };
const formatDateTime = (date: Date) => { const pad = (value: number) => String(value).padStart(2, '0'); return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`; };
const onExpireTimeConfirm = (value: Date) => { addTeamData.value.expireTime = value; showPicker.value = false; };
onMounted(async () => {
  if (!id || id <= 0) { Toast.fail('加载队伍失败'); return; }
  loading.value = true;
  try {
    const res = await myAxios.get('/team/get', { params: { id } });
    if (res?.code === 0) addTeamData.value = { ...initFormData, ...res.data, id, expireTime: parseExpireTime(res.data?.expireTime), status: String(res.data?.status ?? 0) };
    else Toast.fail('加载队伍失败，请刷新重试');
  } catch { Toast.fail('加载队伍失败，请刷新重试'); }
  finally { loading.value = false; }
});
const onSubmit = async () => {
  if (submitting.value || loading.value) return;
  if (!addTeamData.value.id) { Toast.fail('队伍信息未加载完成'); return; }
  if (!addTeamData.value.name) { Toast.fail('请输入队伍名'); return; }
  if (Number(addTeamData.value.status) === 2 && !addTeamData.value.password) { Toast.fail('请填写密码'); return; }
  const status = Number(addTeamData.value.status);
  if (Number.isNaN(status)) { Toast.fail('请选择队伍状态'); return; }
  submitting.value = true;
  try {
    const res = await myAxios.post('/team/update', { ...addTeamData.value, id: Number(addTeamData.value.id), status });
    if (res?.code === 0 && res.data) { Toast.success('更新成功'); router.push({ path: '/team', replace: true }); }
    else Toast.fail(res?.description || '更新失败');
  } catch { Toast.fail('更新失败'); }
  finally { submitting.value = false; }
};
</script>

<style scoped>
.team-form-page{min-height:100%;box-sizing:border-box;padding:28px 16px 116px;color:#f8fafc;background:#0f172a}.form-header{position:relative;display:flex;align-items:center;justify-content:space-between;height:48px;margin-bottom:28px}.form-header button{display:grid;width:48px;height:48px;padding:0;place-items:center;border:0;color:#f8fafc;background:transparent}.form-header h1{position:absolute;left:50%;margin:0;transform:translateX(-50%);font-size:22px;font-weight:700;line-height:30px;white-space:nowrap}.form-header>span{width:48px}.team-form--loading .form-card{opacity:.62;pointer-events:none}.form-card{display:grid;gap:24px;padding:24px;border:1px solid rgba(100,116,139,.5);border-radius:16px;background:#1e293b;box-shadow:0 18px 35px rgba(2,6,23,.22);transition:opacity .2s ease}.form-row{display:flex;align-items:center;gap:16px}.form-row--textarea{align-items:flex-start}.form-row>label{flex:0 0 80px;color:#cbd5e1;font-size:14px;font-weight:500}.form-row--textarea>label{padding-top:12px}.form-control{min-width:0;flex:1;height:44px;box-sizing:border-box;padding:0 16px;border:1px solid #334155;border-radius:12px;outline:0;color:#f8fafc;background:#0f172a;font:inherit;font-size:14px;transition:border-color .15s ease,box-shadow .15s ease}.form-control:focus{border-color:#fff;box-shadow:0 0 0 1px #fff}.form-control::placeholder,.placeholder{color:#94a3b8}textarea.form-control{height:auto;min-height:112px;padding:14px 16px;resize:none}.date-control{display:flex;align-items:center;justify-content:space-between;text-align:left}.date-control span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.date-control svg{flex:0 0 auto;color:#94a3b8}.status-options{display:flex;min-width:0;flex:1;align-items:center;gap:22px}.radio-option{display:flex;align-items:center;gap:10px;color:#f8fafc;font-size:14px;white-space:nowrap}.radio-option input{display:grid;width:20px;height:20px;margin:0;appearance:none;place-content:center;border:2px solid #94a3b8;border-radius:50%;background:transparent}.radio-option input::before{width:10px;height:10px;border-radius:50%;background:#fff;content:'';transform:scale(0);transition:transform .12s ease}.radio-option input:checked{border-color:#fff}.radio-option input:checked::before{transform:scale(1)}.fixed-submit{position:fixed;right:0;bottom:calc(64px + env(safe-area-inset-bottom));left:0;z-index:90;padding:28px 16px 22px;background:linear-gradient(to top,#0f172a 56%,rgba(15,23,42,0))}.fixed-submit button{display:flex;width:100%;height:56px;align-items:center;justify-content:center;gap:10px;border:0;border-radius:999px;color:#fff;background:#3b82f6;font:inherit;font-weight:600;box-shadow:0 16px 30px rgba(37,99,235,.3)}.fixed-submit button:disabled{opacity:.7}.loading-icon{animation:spin 1s linear infinite}.team-picker-popup{overflow:hidden;background:#1e293b}.team-picker-popup :deep(.van-picker),.team-picker-popup :deep(.van-picker__toolbar),.team-picker-popup :deep(.van-picker__columns){background:#1e293b}.team-picker-popup :deep(.van-picker__title),.team-picker-popup :deep(.van-picker-column__item--selected){color:#f8fafc}.team-picker-popup :deep(.van-picker-column__item){color:#94a3b8}@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:360px){.form-card{padding:20px 16px}.form-row{gap:10px}.form-row>label{flex-basis:68px}.status-options{gap:12px}}
</style>


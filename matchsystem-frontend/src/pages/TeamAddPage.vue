<template>
  <div class="team-form-page">
    <header class="form-header">
      <button type="button" aria-label="返回" @click="router.back()"><ChevronLeft :size="28" /></button>
      <h1>创建队伍</h1>
      <span />
    </header>

    <form class="team-form" @submit.prevent="onSubmit">
      <div class="form-card">
        <div class="form-row">
          <label for="addTeamName">队伍名</label>
          <input id="addTeamName" v-model.trim="addTeamData.name" class="form-control" type="text" placeholder="请输入队伍名" />
        </div>

        <div class="form-row form-row--textarea">
          <label for="addTeamDescription">队伍描述</label>
          <textarea id="addTeamDescription" v-model.trim="addTeamData.description" class="form-control" rows="4" placeholder="请输入队伍描述" />
        </div>

        <div class="form-row">
          <label>过期时间</label>
          <button class="form-control date-control" type="button" @click="showPicker = true">
            <span :class="{ placeholder: !expireTimeText }">{{ expireTimeText || '点击选择过期时间' }}</span>
            <ChevronRight :size="20" />
          </button>
        </div>

        <div class="form-row">
          <label>最大人数</label>
          <div class="member-stepper">
            <button type="button" aria-label="减少人数" @click="changeMaxNum(-1)"><Minus :size="20" /></button>
            <strong>{{ addTeamData.maxNum }}</strong>
            <button type="button" aria-label="增加人数" @click="changeMaxNum(1)"><Plus :size="20" /></button>
          </div>
        </div>

        <div class="form-row">
          <label>队伍状态</label>
          <div class="status-options">
            <label v-for="option in statusOptions" :key="option.value" class="radio-option">
              <input v-model="addTeamData.status" type="radio" name="addTeamStatus" :value="option.value" />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>

        <div v-if="Number(addTeamData.status) === 2" class="form-row">
          <label for="addTeamPassword">密码</label>
          <input id="addTeamPassword" v-model="addTeamData.password" class="form-control" type="password" placeholder="请输入队伍密码" />
        </div>
      </div>

      <div class="fixed-submit">
        <button type="submit" :disabled="submitting">
          <LoaderCircle v-if="submitting" class="loading-icon" :size="20" />
          <span>{{ submitting ? '正在提交...' : '提交' }}</span>
        </button>
      </div>
    </form>

    <van-popup v-model:show="showPicker" round position="bottom" class="team-picker-popup">
      <van-datetime-picker v-model="addTeamData.expireTime" type="datetime" title="请选择过期时间" :min-date="minDate" @confirm="onExpireTimeConfirm" @cancel="showPicker = false" />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ChevronLeft, ChevronRight, LoaderCircle, Minus, Plus } from 'lucide-vue-next';
import { Toast } from 'vant';
import myAxios from '../plugins/myAxios';

type TeamAddForm = { name: string; description: string; expireTime: Date | null; maxNum: number; password: string; status: string; };
const router = useRouter();
const showPicker = ref(false);
const submitting = ref(false);
const minDate = new Date();
const statusOptions = [{ label: '公开', value: '0' }, { label: '私有', value: '1' }, { label: '加密', value: '2' }];
const addTeamData = ref<TeamAddForm>({ name: '', description: '', expireTime: null, maxNum: 3, password: '', status: '0' });
const expireTimeText = computed(() => addTeamData.value.expireTime ? formatDateTime(addTeamData.value.expireTime) : '');
const formatDateTime = (date: Date) => { const pad = (value: number) => String(value).padStart(2, '0'); return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`; };
const onExpireTimeConfirm = (value: Date) => { addTeamData.value.expireTime = value; showPicker.value = false; };
const changeMaxNum = (step: number) => { addTeamData.value.maxNum = Math.min(10, Math.max(3, addTeamData.value.maxNum + step)); };
const onSubmit = async () => {
  if (submitting.value) return;
  if (!addTeamData.value.name) { Toast.fail('请输入队伍名'); return; }
  if (Number(addTeamData.value.status) === 2 && !addTeamData.value.password) { Toast.fail('请填写密码'); return; }
  submitting.value = true;
  try {
    const res = await myAxios.post('/team/add', { ...addTeamData.value, status: Number(addTeamData.value.status) });
    if (res?.code === 0 && res.data) { Toast.success('添加成功'); router.push({ path: '/team', replace: true }); }
    else Toast.fail(res?.description || '添加失败');
  } catch { Toast.fail('添加失败'); }
  finally { submitting.value = false; }
};
</script>

<style scoped>
.team-form-page{min-height:100%;box-sizing:border-box;padding:28px 16px 116px;color:#f8fafc;background:#0f172a}.form-header{position:relative;display:flex;align-items:center;justify-content:space-between;height:48px;margin-bottom:28px}.form-header button{display:grid;width:48px;height:48px;padding:0;place-items:center;border:0;color:#f8fafc;background:transparent}.form-header h1{position:absolute;left:50%;margin:0;transform:translateX(-50%);font-size:22px;font-weight:700;line-height:30px;white-space:nowrap}.form-header>span{width:48px}.form-card{display:grid;gap:24px;padding:24px;border:1px solid rgba(100,116,139,.5);border-radius:16px;background:#1e293b;box-shadow:0 18px 35px rgba(2,6,23,.22)}.form-row{display:flex;align-items:center;gap:16px}.form-row--textarea{align-items:flex-start}.form-row>label,.form-row>span{flex:0 0 80px;color:#cbd5e1;font-size:14px;font-weight:500}.form-row--textarea>label{padding-top:12px}.form-control{min-width:0;flex:1;height:44px;box-sizing:border-box;padding:0 16px;border:1px solid #334155;border-radius:12px;outline:0;color:#f8fafc;background:#0f172a;font:inherit;font-size:14px;transition:border-color .15s ease,box-shadow .15s ease}.form-control:focus{border-color:#fff;box-shadow:0 0 0 1px #fff}.form-control::placeholder,.placeholder{color:#94a3b8}textarea.form-control{height:auto;min-height:112px;padding:14px 16px;resize:none}.date-control{display:flex;align-items:center;justify-content:space-between;text-align:left}.date-control span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.date-control svg{flex:0 0 auto;color:#94a3b8}.member-stepper{display:flex;min-width:0;flex:1;height:44px;overflow:hidden;border:1px solid #334155;border-radius:12px;background:#0f172a}.member-stepper button{display:grid;width:48px;padding:0;place-items:center;border:0;color:#94a3b8;background:transparent}.member-stepper button:first-child{border-right:1px solid #334155}.member-stepper button:last-child{border-left:1px solid #334155}.member-stepper strong{display:grid;flex:1;place-items:center;color:#f8fafc;font-size:14px}.status-options{display:flex;min-width:0;flex:1;align-items:center;gap:22px}.radio-option{display:flex;align-items:center;gap:10px;color:#f8fafc;font-size:14px;white-space:nowrap}.radio-option input{display:grid;width:20px;height:20px;margin:0;appearance:none;place-content:center;border:2px solid #94a3b8;border-radius:50%;background:transparent}.radio-option input::before{width:10px;height:10px;border-radius:50%;background:#fff;content:'';transform:scale(0);transition:transform .12s ease}.radio-option input:checked{border-color:#fff}.radio-option input:checked::before{transform:scale(1)}.fixed-submit{position:fixed;right:0;bottom:calc(64px + env(safe-area-inset-bottom));left:0;z-index:90;padding:28px 16px 22px;background:linear-gradient(to top,#0f172a 56%,rgba(15,23,42,0))}.fixed-submit button{display:flex;width:100%;height:56px;align-items:center;justify-content:center;gap:10px;border:0;border-radius:999px;color:#fff;background:#3b82f6;font:inherit;font-weight:600;box-shadow:0 16px 30px rgba(37,99,235,.3)}.fixed-submit button:disabled{opacity:.7}.loading-icon{animation:spin 1s linear infinite}.team-picker-popup{overflow:hidden;background:#1e293b}.team-picker-popup :deep(.van-picker),.team-picker-popup :deep(.van-picker__toolbar),.team-picker-popup :deep(.van-picker__columns){background:#1e293b}.team-picker-popup :deep(.van-picker__title),.team-picker-popup :deep(.van-picker-column__item--selected){color:#f8fafc}.team-picker-popup :deep(.van-picker-column__item){color:#94a3b8}@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:360px){.form-card{padding:20px 16px}.form-row{gap:10px}.form-row>label{flex-basis:68px}.status-options{gap:12px}}
</style>


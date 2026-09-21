<template>
  <section class="user-update-page">
    <span class="background-glow background-glow--blue" />
    <span class="background-glow background-glow--purple" />

    <main v-if="user" class="profile-panel">
      <section class="profile-hero">
        <button class="avatar-button" type="button" aria-label="更换头像" @click="openAvatarEditor">
          <img :src="user.avatarUrl || defaultAvatar" :alt="user.username || user.userAccount" />
          <span class="avatar-button__overlay"><Camera :size="30" :stroke-width="1.8" /></span>
          <span class="avatar-button__hint"><Camera :size="15" :stroke-width="2" />更换头像</span>
        </button>

        <button class="profile-name-button" type="button" @click="openTextEditor('username', '昵称', user.username || '')">
          <h2>{{ user.username || user.userAccount }}</h2>
          <Pencil :size="19" :stroke-width="1.9" />
        </button>

        <div class="profile-identifiers">
          <span>账号: <b>{{ user.userAccount || '未设置' }}</b></span>
          <span>编号: <b>{{ user.planetCode || '未设置' }}</b></span>
        </div>
      </section>

      <div class="information-stack">
        <section class="information-group">
          <h3><Contact :size="16" :stroke-width="1.8" />核心联系信息</h3>
          <button class="information-row" type="button" @click="openTextEditor('phone', '电话', user.phone || '')">
            <span>电话</span>
            <span class="information-row__value"><b>{{ user.phone || '未设置' }}</b><ChevronRight :size="20" :stroke-width="1.8" /></span>
          </button>
          <button class="information-row" type="button" @click="openTextEditor('email', '邮箱', user.email || '')">
            <span>邮箱</span>
            <span class="information-row__value"><b class="information-row__ellipsis">{{ user.email || '未设置' }}</b><ChevronRight :size="20" :stroke-width="1.8" /></span>
          </button>
        </section>

        <section class="information-group information-group--divided">
          <h3><Tags :size="16" :stroke-width="1.8" />个性化标签</h3>
          <button class="information-row" type="button" @click="openGenderEditor">
            <span>性别</span>
            <span class="information-row__value"><b>{{ getGenderText(user.gender) }}</b><ChevronRight :size="20" :stroke-width="1.8" /></span>
          </button>
          <button class="information-row information-row--tags" type="button" @click="openTextEditor('tags', '标签', tagsToText(user.tags))">
            <span>标签</span>
            <span class="information-row__value">
              <span v-if="normalizeTags(user.tags).length" class="tag-preview">
                <span v-for="tag in normalizeTags(user.tags).slice(0, 3)" :key="tag">{{ tag }}</span>
                <em v-if="normalizeTags(user.tags).length > 3">+{{ normalizeTags(user.tags).length - 3 }}</em>
              </span>
              <b v-else>未设置</b>
              <ChevronRight :size="20" :stroke-width="1.8" />
            </span>
          </button>
        </section>

        <section class="information-group information-group--divided">
          <h3><ShieldCheck :size="16" :stroke-width="1.8" />账户状态</h3>
          <div class="information-row information-row--static">
            <span>注册时间</span>
            <span class="information-row__value"><b>{{ formatDate(user.createTime) }}</b></span>
          </div>
        </section>
      </div>
    </main>

    <van-popup v-model:show="showEditPopup" class="mall-edit-popup" position="bottom" round>
      <div class="edit-card">
        <header class="edit-card-header">
          <div><span>编辑资料</span><h2>{{ editTitle }}</h2></div>
          <button type="button" aria-label="关闭编辑" @click="closeEditor"><X :size="22" :stroke-width="2" /></button>
        </header>

        <div class="edit-card-body">
          <template v-if="editType === 'avatar'">
            <div class="avatar-edit-preview"><img :src="avatarPreview || user?.avatarUrl || defaultAvatar" alt="头像预览" /></div>
            <div class="avatar-upload-actions">
              <van-uploader v-model="avatarCameraFileList" accept="image/*" capture="environment" :max-count="1" :before-read="beforeAvatarRead" :after-read="afterAvatarRead">
                <button class="avatar-upload-button" type="button"><Camera :size="18" />拍照上传</button>
              </van-uploader>
              <van-uploader v-model="avatarFileList" accept="image/*" :max-count="1" :before-read="beforeAvatarRead" :after-read="afterAvatarRead">
                <button class="avatar-upload-button" type="button"><Upload :size="18" />文件上传</button>
              </van-uploader>
            </div>
          </template>

          <template v-else-if="editType === 'gender'">
            <van-radio-group v-model="editValue" class="gender-radio-group" direction="horizontal">
              <van-radio name="1">男</van-radio>
              <van-radio name="2">女</van-radio>
            </van-radio-group>
          </template>

          <template v-else>
            <van-field v-model="editValue" class="mall-edit-field" :placeholder="placeholderText" :type="activeField === 'tags' ? 'textarea' : 'text'" :rows="activeField === 'tags' ? 3 : 1" autosize />
          </template>
        </div>

        <div class="edit-card-actions">
          <button class="edit-cancel-button" type="button" :disabled="saving" @click="closeEditor">取消</button>
          <button class="edit-save-button" type="button" :disabled="saving" @click="saveEdit">
            <van-loading v-if="saving" color="#0f172a" size="18" />
            <span v-else>保存修改</span>
          </button>
        </div>
      </div>
    </van-popup>
  </section>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
import {Camera, ChevronRight, Contact, Pencil, ShieldCheck, Tags, Upload, X} from 'lucide-vue-next';
import {getCurrentUser} from "../services/user";
import {setCurrentUserState} from "../states/user";

type EditField = 'username' | 'avatarUrl' | 'gender' | 'phone' | 'email' | 'tags';
type EditType = 'text' | 'avatar' | 'gender';

const user = ref<any>();
const showEditPopup = ref(false);
const activeField = ref<EditField>('username');
const editType = ref<EditType>('text');
const editTitle = ref('');
const editValue = ref('');
const saving = ref(false);
const avatarFileList = ref<any[]>([]);
const avatarCameraFileList = ref<any[]>([]);
const selectedAvatarFile = ref<File | null>(null);
const avatarPreview = ref('');
const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg';

const getGenderText = (value: number) => {
  if (value === 1) {
    return '男';
  }
  if (value === 2) {
    return '女';
  }
  return '未设置';
}

const normalizeTags = (rawTags: unknown): string[] => {
  if (!rawTags) {
    return [];
  }
  if (Array.isArray(rawTags)) {
    return rawTags.map(tag => String(tag).trim()).filter(Boolean);
  }
  const tagsText = String(rawTags).trim();
  if (!tagsText) {
    return [];
  }
  try {
    const parsedTags = JSON.parse(tagsText);
    return Array.isArray(parsedTags)
      ? parsedTags.map(tag => String(tag).trim()).filter(Boolean)
      : [String(parsedTags).trim()].filter(Boolean);
  } catch (error) {
    return tagsText
      .split(/[,，]/)
      .map(tag => tag.trim())
      .filter(Boolean);
  }
}

const formatTags = (rawTags: unknown) => {
  const tags = normalizeTags(rawTags);
  return tags.length > 0 ? tags.join(', ') : '未设置';
}

const tagsToText = (rawTags: unknown) => {
  return normalizeTags(rawTags).join(', ');
}

const textToTagsJson = (value: unknown) => {
  const tags = String(value ?? '')
    .split(/[,，]/)
    .map(tag => tag.trim())
    .filter(Boolean);
  return JSON.stringify(tags);
}

const formatDate = (value: unknown) => {
  if (!value) {
    return '未设置';
  }
  const date = new Date(value as string | number | Date);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const placeholderText = computed(() => {
  if (activeField.value === 'tags') {
    return '请输入标签，多个标签用逗号分隔';
  }
  return `请输入${editTitle.value}`;
})

onMounted(async () => {
  user.value = await getCurrentUser();
})

const openTextEditor = (field: Exclude<EditField, 'avatarUrl' | 'gender'>, title: string, currentValue: string) => {
  activeField.value = field;
  editType.value = 'text';
  editTitle.value = title;
  editValue.value = currentValue;
  showEditPopup.value = true;
}

const openGenderEditor = () => {
  activeField.value = 'gender';
  editType.value = 'gender';
  editTitle.value = '性别';
  editValue.value = user.value?.gender ? String(user.value.gender) : '1';
  showEditPopup.value = true;
}

const openAvatarEditor = () => {
  activeField.value = 'avatarUrl';
  editType.value = 'avatar';
  editTitle.value = '头像';
  editValue.value = user.value?.avatarUrl || '';
  avatarPreview.value = user.value?.avatarUrl || '';
  avatarFileList.value = [];
  avatarCameraFileList.value = [];
  selectedAvatarFile.value = null;
  showEditPopup.value = true;
}

const closeEditor = () => {
  if (saving.value) {
    return;
  }
  showEditPopup.value = false;
}

const beforeAvatarRead = (file: File | File[]) => {
  const avatarFile = Array.isArray(file) ? file[0] : file;
  if (!avatarFile.type.startsWith('image/')) {
    Toast.fail('请选择图片文件');
    return false;
  }
  if (avatarFile.size > 2 * 1024 * 1024) {
    Toast.fail('头像文件不能超过 2MB');
    return false;
  }
  return true;
}

const afterAvatarRead = (fileItem: any) => {
  const currentFileItem = Array.isArray(fileItem) ? fileItem[0] : fileItem;
  selectedAvatarFile.value = currentFileItem?.file || null;
  avatarPreview.value = currentFileItem?.content || avatarPreview.value;
}

const uploadAvatar = async () => {
  if (!selectedAvatarFile.value) {
    Toast.fail('请选择头像文件');
    return '';
  }
  const formData = new FormData();
  formData.append('file', selectedAvatarFile.value);
  const res = await myAxios.post('/file/upload/avatar', formData);
  if (res.code === 0 && res.data) {
    return res.data;
  }
  Toast.fail(res.description || '头像上传失败');
  return '';
}

const refreshMatchWithoutRedisAfterTagsUpdate = () => {
  myAxios.get('/user/match/withoutRedis', {
    params: {
      num: 30,
    },
  }).catch((error) => {
    console.error('/user/match/withoutRedis error', error);
  });
}

const saveEdit = async () => {
  if (!user.value) {
    Toast.fail('用户未登录');
    return;
  }
  const updatedField = activeField.value;
  saving.value = true;
  try {
    let submitValue: string | number = editValue.value;
    if (updatedField === 'gender') {
      submitValue = Number(editValue.value);
    }
    if (updatedField === 'tags') {
      submitValue = textToTagsJson(editValue.value);
    }
    if (updatedField === 'avatarUrl') {
      const avatarUrl = await uploadAvatar();
      if (!avatarUrl) {
        return;
      }
      submitValue = avatarUrl;
    }

    const res = await myAxios.post('/user/update', {
      id: user.value.id,
      [updatedField]: submitValue,
    });
    if (res.code === 0 && res.data > 0) {
      user.value = {
        ...user.value,
        [updatedField]: submitValue,
      };
      setCurrentUserState(user.value);
      Toast.success('修改成功');
      showEditPopup.value = false;
      if (updatedField === 'tags') {
        refreshMatchWithoutRedisAfterTagsUpdate();
      }
      return;
    }
    Toast.fail(res.description || '修改错误');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.user-update-page{position:relative;box-sizing:border-box;width:100%;min-height:100%;padding:18px 16px 34px;overflow:hidden;color:#f8fafc;background:#0f172a;font-family:Inter,"PingFang SC","Microsoft YaHei",sans-serif}.background-glow{position:absolute;z-index:0;width:280px;height:280px;border-radius:50%;opacity:.12;filter:blur(90px);pointer-events:none}.background-glow--blue{top:8%;left:-120px;background:#3b82f6}.background-glow--purple{right:-140px;bottom:4%;background:#8b5cf6}.profile-panel{position:relative;z-index:1;width:100%;max-width:520px;box-sizing:border-box;margin:0 auto;padding:24px 20px 26px;border:1px solid rgba(255,255,255,.1);border-radius:24px;background:rgba(255,255,255,.05);box-shadow:0 20px 42px rgba(2,6,23,.35);backdrop-filter:blur(20px);-webkit-backdrop-filter:blur(20px)}.profile-hero{display:flex;flex-direction:column;align-items:center;margin-bottom:42px}.avatar-button{position:relative;width:128px;height:128px;margin:0 0 22px;padding:0;border:4px solid rgba(255,255,255,.1);border-radius:50%;background:#1e293b;box-shadow:0 18px 36px rgba(2,6,23,.42),0 0 28px rgba(59,130,246,.14)}.avatar-button img{display:block;width:100%;height:100%;border-radius:50%;object-fit:cover}.avatar-button__overlay{position:absolute;inset:0;display:grid;place-items:center;border-radius:50%;color:#fff;background:rgba(2,6,23,.5);opacity:0;transition:opacity .18s ease}.avatar-button__hint{position:absolute;right:-6px;bottom:4px;display:flex;height:30px;align-items:center;gap:5px;padding:0 10px;border:2px solid #0f172a;border-radius:999px;color:#fff;background:#3b82f6;font-size:10px;font-weight:600;white-space:nowrap}.avatar-button:active .avatar-button__overlay{opacity:1}.profile-name-button{display:flex;max-width:100%;align-items:center;gap:10px;padding:0;border:0;color:#f8fafc;background:transparent}.profile-name-button h2{max-width:calc(100vw - 116px);margin:0;overflow:hidden;font-size:28px;font-weight:800;line-height:36px;letter-spacing:-.03em;text-overflow:ellipsis;white-space:nowrap}.profile-name-button svg{flex:0 0 auto;color:#94a3b8}.profile-identifiers{display:flex;flex-direction:column;align-items:center;gap:5px;margin-top:11px;color:#64748b;font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:11px;line-height:16px}.profile-identifiers b{color:#cbd5e1;font-weight:500}.information-stack{display:grid;gap:24px}.information-group{display:grid;gap:12px}.information-group--divided{padding-top:22px;border-top:1px solid rgba(255,255,255,.06)}.information-group h3{display:flex;align-items:center;gap:8px;margin:0;padding-left:8px;color:#94a3b8;font-size:12px;font-weight:600;line-height:18px}.information-row{display:flex;width:100%;min-height:58px;box-sizing:border-box;align-items:center;justify-content:space-between;gap:16px;padding:14px 16px;border:1px solid rgba(255,255,255,.06);border-radius:16px;color:#cbd5e1;background:rgba(255,255,255,.045);box-shadow:0 8px 18px rgba(2,6,23,.16);font:inherit;font-size:14px;font-weight:500;text-align:left;transition:transform .15s ease,background .18s ease}button.information-row:active{transform:scale(.985);background:rgba(255,255,255,.08)}.information-row__value{display:flex;min-width:0;flex:1;align-items:center;justify-content:flex-end;gap:10px;color:#94a3b8}.information-row__value b{min-width:0;color:#f8fafc;font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:12px;font-weight:500}.information-row__value svg{flex:0 0 auto;color:#64748b}.information-row__ellipsis{max-width:min(54vw,230px);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.information-row--tags{align-items:flex-start}.tag-preview{display:flex;min-width:0;flex-wrap:wrap;justify-content:flex-end;gap:6px}.tag-preview span,.tag-preview em{padding:5px 10px;border-radius:999px;color:#60a5fa;background:#334155;font-size:10px;font-style:normal;font-weight:600;line-height:14px}.information-row--static{cursor:default}.mall-edit-popup{background:transparent}.edit-card{box-sizing:border-box;width:100%;padding:20px 18px calc(22px + env(safe-area-inset-bottom));border:1px solid #334155;border-bottom:0;border-radius:24px 24px 0 0;color:#f8fafc;background:#1e293b;box-shadow:0 -20px 45px rgba(2,6,23,.46)}.edit-card-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px}.edit-card-header span{color:#60a5fa;font-size:10px;font-weight:700;letter-spacing:.14em}.edit-card-header h2{margin:3px 0 0;font-size:20px;line-height:28px}.edit-card-header button{display:grid;width:40px;height:40px;padding:0;place-items:center;border:0;border-radius:50%;color:#94a3b8;background:#334155}.edit-card-body{margin-bottom:20px}.avatar-edit-preview{display:flex;justify-content:center;margin-bottom:18px}.avatar-edit-preview img{width:112px;height:112px;box-sizing:border-box;border:4px solid rgba(255,255,255,.1);border-radius:50%;object-fit:cover;background:#0f172a}.avatar-upload-actions{display:grid;grid-template-columns:1fr 1fr;gap:12px}.avatar-upload-actions :deep(.van-uploader),.avatar-upload-actions :deep(.van-uploader__wrapper),.avatar-upload-actions :deep(.van-uploader__input-wrapper){width:100%}.avatar-upload-button{display:flex;width:100%;height:46px;align-items:center;justify-content:center;gap:8px;border:1px solid #475569;border-radius:14px;color:#f8fafc;background:#334155;font:inherit;font-size:13px;font-weight:600}.mall-edit-field{box-sizing:border-box;width:100%;padding:14px 16px;border:1px solid #475569;border-radius:16px;background:#0f172a}.mall-edit-field :deep(.van-field__control){color:#f8fafc;font-size:15px}.mall-edit-field :deep(.van-field__control::placeholder){color:#64748b}.gender-radio-group{display:flex;box-sizing:border-box;justify-content:center;gap:46px;padding:22px 14px;border:1px solid #475569;border-radius:16px;background:#0f172a}.gender-radio-group :deep(.van-radio__label){color:#f8fafc}.edit-card-actions{display:grid;grid-template-columns:1fr 1fr;gap:12px}.edit-cancel-button,.edit-save-button{height:48px;border-radius:999px;font:inherit;font-size:14px;font-weight:700}.edit-cancel-button{border:1px solid #475569;color:#cbd5e1;background:transparent}.edit-save-button{display:grid;place-items:center;border:0;color:#0f172a;background:#fff}.edit-cancel-button:disabled,.edit-save-button:disabled{opacity:.65}@media(max-width:359px){.user-update-page{padding-right:12px;padding-left:12px}.profile-panel{padding-right:14px;padding-left:14px}.avatar-upload-actions{grid-template-columns:1fr}}
</style>

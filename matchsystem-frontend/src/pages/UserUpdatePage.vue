<template>
  <div class="mall-page user-update-page">
    <div class="user-update-list" v-if="user">
      <van-cell
        class="mall-prof-cell"
        title="昵称"
        is-link
        :value="user.username"
        @click="openTextEditor('username', '昵称', user.username || '')"
      />
      <van-cell class="mall-prof-cell" title="账号" :value="user.userAccount"/>
      <van-cell
        class="mall-prof-cell"
        title="头像"
        is-link
        @click="openAvatarEditor"
      >
        <img class="mall-avatar" :src="user.avatarUrl || defaultAvatar" alt="头像"/>
      </van-cell>
      <van-cell
        class="mall-prof-cell"
        title="性别"
        is-link
        :value="getGenderText(user.gender)"
        @click="openGenderEditor"
      />
      <van-cell
        class="mall-prof-cell"
        title="电话"
        is-link
        :value="user.phone"
        @click="openTextEditor('phone', '电话', user.phone || '')"
      />
      <van-cell
        class="mall-prof-cell"
        title="邮箱"
        is-link
        :value="user.email"
        @click="openTextEditor('email', '邮箱', user.email || '')"
      />
      <van-cell
        class="mall-prof-cell"
        title="标签"
        is-link
        :value="formatTags(user.tags)"
        @click="openTextEditor('tags', '标签', tagsToText(user.tags))"
      />
      <van-cell class="mall-prof-cell" title="用户编号" :value="user.planetCode"/>
      <van-cell class="mall-prof-cell" title="注册时间" :value="formatDate(user.createTime)"/>
    </div>

    <van-popup
      v-model:show="showEditPopup"
      class="mall-edit-popup"
      position="bottom"
      round
    >
      <div class="edit-card">
        <div class="edit-card-header">
          <div class="edit-card-title">{{ editTitle }}</div>
          <van-icon name="cross" size="20" @click="closeEditor"/>
        </div>

        <div class="edit-card-body">
          <template v-if="editType === 'avatar'">
            <div class="avatar-edit-preview">
              <van-image
                round
                width="7rem"
                height="7rem"
                :src="avatarPreview || user?.avatarUrl || defaultAvatar"
              />
            </div>
            <div class="avatar-upload-actions">
              <van-uploader
                v-model="avatarCameraFileList"
                accept="image/*"
                capture="environment"
                :max-count="1"
                :before-read="beforeAvatarRead"
                :after-read="afterAvatarRead"
              >
                <van-button class="avatar-upload-btn" plain type="primary" icon="photograph">拍照上传</van-button>
              </van-uploader>
              <van-uploader
                v-model="avatarFileList"
                accept="image/*"
                :max-count="1"
                :before-read="beforeAvatarRead"
                :after-read="afterAvatarRead"
              >
                <van-button class="avatar-upload-btn" plain type="primary" icon="photo-o">文件上传</van-button>
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
            <van-field
              v-model="editValue"
              class="mall-edit-field"
              :label="editTitle"
              :placeholder="placeholderText"
              :type="activeField === 'tags' ? 'textarea' : 'text'"
              :rows="activeField === 'tags' ? 2 : 1"
              autosize
            />
          </template>
        </div>

        <div class="edit-card-actions">
          <van-button class="edit-cancel-btn" block plain @click="closeEditor">取消</van-button>
          <van-button class="edit-save-btn" block type="primary" :loading="saving" @click="saveEdit">保存</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";
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
.user-update-page {
  box-sizing: border-box;
  min-height: 100%;
  padding: 8px 0 4px;
}

.user-update-list {
  box-sizing: border-box;
  width: 100%;
}

.user-update-page :deep(.van-cell) {
  box-sizing: border-box;
  width: calc(100% - 24px);
  border: 1px solid var(--border-weak);
  border-radius: 12px;
  margin: 8px 12px;
}

.user-update-page :deep(.van-cell__title) {
  color: var(--text-main);
}

.user-update-page :deep(.van-cell__value) {
  min-width: 0;
  overflow: hidden;
  color: var(--text-light-1);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mall-avatar {
  box-sizing: border-box;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.mall-edit-popup {
  background: transparent;
}

.edit-card {
  box-sizing: border-box;
  width: 100%;
  padding: 16px 16px 20px;
  background: var(--bg-panel);
  border: 1px solid var(--border-weak);
  border-radius: 16px 16px 0 0;
}

.edit-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  color: var(--text-main);
}

.edit-card-title {
  font-size: 17px;
  font-weight: 600;
}

.edit-card-body {
  margin-bottom: 18px;
}

.mall-edit-field {
  box-sizing: border-box;
  width: 100%;
  background: var(--bg-elevated);
  border: 1px solid var(--border-weak);
  border-radius: 10px;
}

.mall-edit-field :deep(.van-field__control) {
  color: var(--text-main);
}

.mall-edit-field :deep(.van-field__label) {
  color: var(--text-light);
}

.avatar-edit-preview {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar-upload-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.avatar-upload-btn {
  min-width: 112px;
  border-radius: 10px;
}

.gender-radio-group {
  box-sizing: border-box;
  justify-content: center;
  padding: 18px 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-weak);
  border-radius: 10px;
  gap: 36px;
}

.edit-card-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.edit-cancel-btn,
.edit-save-btn {
  border-radius: 10px;
}
</style>

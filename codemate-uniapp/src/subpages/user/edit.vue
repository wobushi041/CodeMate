<template>
  <view class="user-edit">
    <view class="user-edit__avatar-section">
      <image v-if="avatarPreview" class="user-edit__avatar" :src="avatarPreview" mode="aspectFill" />
      <view v-else class="user-edit__avatar user-edit__avatar--fallback">{{ (form.username || 'U').slice(0, 1) }}</view>
      <button @click="chooseAvatar">选择新头像</button>
    </view>

    <view class="user-edit__field">
      <text>昵称</text>
      <input v-model="form.username" placeholder="请输入昵称" />
    </view>
    <view class="user-edit__field">
      <text>个人简介</text>
      <textarea v-model="form.profile" placeholder="介绍一下自己" />
    </view>
    <view class="user-edit__field">
      <text>性别</text>
      <view class="user-edit__gender">
        <button :class="{ active: form.gender === 1 }" @click="form.gender = 1">男</button>
        <button :class="{ active: form.gender === 0 }" @click="form.gender = 0">女</button>
      </view>
    </view>
    <view class="user-edit__field">
      <text>手机号</text>
      <input v-model="form.phone" type="number" :maxlength="11" placeholder="请输入11位手机号" />
    </view>
    <view class="user-edit__field">
      <text>邮箱</text>
      <input v-model="form.email" placeholder="请输入邮箱" />
    </view>
    <view class="user-edit__field user-edit__field--tags">
      <text>技术标签（逗号分隔）</text>
      <textarea v-model="tagsText" placeholder="例如：Vue, Java, Spring Boot" />
      <view class="user-edit__quick-tags">
        <text
          v-for="tag in quickTags"
          :key="tag"
          :class="{ active: currentTags.includes(tag) }"
          @click="toggleTag(tag)"
        >{{ tag }}</text>
      </view>
    </view>

    <button class="user-edit__submit" :loading="saving" :disabled="saving" @click="save">保存修改</button>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { uploadAvatar } from '@/api/file'
import { refreshMatchedUsers, updateUser } from '@/api/partner'
import { useUserStore } from '@/store/user'
import { normalizeTags } from '@/utils/format'
import { requireLogin } from '@/utils/navigation'

const userStore = useUserStore()
const saving = ref(false)
const avatarPreview = ref('')
const selectedAvatarPath = ref('')
const tagsText = ref('')
const initialized = ref(false)
const loadedSnapshot = ref('')
const quickTags = ['Vue', 'React', 'Java', 'Python', 'Spring Boot', 'MySQL', 'Redis', 'Docker', 'AI', '前端开发', '后端开发']
const form = reactive({
  username: '',
  profile: '',
  gender: 0,
  phone: '',
  email: '',
})
const currentTags = computed(() => normalizeTags(tagsText.value))

function formSnapshot(): string {
  return JSON.stringify({ ...form, tagsText: tagsText.value })
}

function hasLocalChanges(): boolean {
  return Boolean(selectedAvatarPath.value) || formSnapshot() !== loadedSnapshot.value
}

onLoad(() => {
  if (!requireLogin()) return
  fillForm()
  refreshUser()
})

function fillForm() {
  const user = userStore.userInfo
  if (!user) return
  form.username = user.username || ''
  form.profile = user.profile || ''
  form.gender = Number(user.gender ?? 0)
  form.phone = user.phone || ''
  form.email = user.email || ''
  tagsText.value = normalizeTags(user.tags).join(', ')
  avatarPreview.value = user.avatarUrl || ''
  loadedSnapshot.value = formSnapshot()
  initialized.value = true
}

async function refreshUser() {
  try {
    await userStore.fetchCurrentUser()
    if (!initialized.value || !hasLocalChanges()) fillForm()
  } catch {
    // 请求层统一提示
  }
}

function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (result) => {
      const files = result.tempFiles
        ? (Array.isArray(result.tempFiles) ? result.tempFiles : [result.tempFiles])
        : []
      const file = files[0]
      if (file?.size && file.size > 2 * 1024 * 1024) {
        uni.showToast({ title: '头像不能超过 2MB', icon: 'none' })
        return
      }
      const paths = Array.isArray(result.tempFilePaths) ? result.tempFilePaths : [result.tempFilePaths]
      const path = (file && 'path' in file ? file.path : '') || paths[0]
      if (path) {
        selectedAvatarPath.value = path
        avatarPreview.value = path
      }
    },
  })
}

function toggleTag(tag: string) {
  const tags = currentTags.value
  const index = tags.indexOf(tag)
  if (index >= 0) tags.splice(index, 1)
  else tags.push(tag)
  tagsText.value = tags.join(', ')
}

async function save() {
  const user = userStore.userInfo
  if (!user?.id || saving.value) return
  const phone = form.phone.trim()
  if (phone && !/^\d{11}$/.test(phone)) {
    uni.showToast({ title: '手机号必须为11位数字', icon: 'none' })
    return
  }
  if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) {
    uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
    return
  }

  saving.value = true
  try {
    let avatarUrl = user.avatarUrl || ''
    if (selectedAvatarPath.value) {
      avatarUrl = await uploadAvatar(selectedAvatarPath.value)
    }
    const tags = normalizeTags(tagsText.value)
    await updateUser({
      id: user.id,
      username: form.username.trim(),
      profile: form.profile.trim(),
      gender: Number(form.gender),
      phone,
      email: form.email.trim(),
      avatarUrl,
      tags: JSON.stringify(tags),
    })
    await userStore.fetchCurrentUser()
    refreshMatchedUsers(20).catch(() => undefined)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 300)
  } catch (error) {
    const message = error instanceof Error ? error.message : '保存失败'
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.user-edit { width: calc(100% - 32px); max-width: 520px; margin: 18px auto 34px; padding: 24px 20px 26px; border: 1px solid rgba(255,255,255,.08); border-radius: 24px; background: #ffffff; box-shadow: 0 20px 42px rgba(2,6,23,.35); }
.user-edit__avatar-section { display: flex; margin-bottom: 30px; flex-direction: column; align-items: center; }
.user-edit__avatar { display: flex; width: 128px; height: 128px; align-items: center; justify-content: center; border: 4px solid rgba(255,255,255,.1); border-radius: 50%; background: #eae7e0; box-shadow: 0 18px 36px rgba(2,6,23,.42),0 0 28px rgba(59,130,246,.14); }
.user-edit__avatar--fallback { color: #242321; font-size: 42px; font-weight: 700; }
.user-edit__avatar-section button { height: 34px; margin: 16px 0 0; padding: 0 16px; border: 1px solid rgba(147,197,253,.58); border-radius: 999px; color: #eff6ff; background: rgba(2,6,23,.5); font-size: 12px; line-height: 32px; }
.user-edit__avatar-section button::after, .user-edit__gender button::after, .user-edit__submit::after { border: 0; }
.user-edit__field { padding: 16px 0; border-bottom: 1px solid rgba(255,255,255,.06); }
.user-edit__field > text { display: block; margin-bottom: 10px; color: #85817a; font-size: 12px; font-weight: 500; }
.user-edit__field input, .user-edit__field textarea { width: 100%; min-height: 42px; padding: 10px 14px; border: 1px solid #eae7e0; border-radius: 12px; color: #242321; background: #faf9f6; font-size: 14px; }
.user-edit__field textarea { min-height: 88px; line-height: 1.6; }
.user-edit__gender { display: flex; gap: 12px; }
.user-edit__gender button { min-height: 40px; margin: 0; padding: 8px 24px; flex: 1; border: 1px solid #eae7e0; border-radius: 12px; color: #85817a; background: #faf9f6; font-size: 14px; }
.user-edit__gender button.active { border-color: #fff; color: #faf9f6; background: #242321; font-weight: 600; }
.user-edit__quick-tags { display: flex; margin-top: 12px; flex-wrap: wrap; gap: 8px; }
.user-edit__quick-tags text { padding: 5px 12px; border-radius: 999px; color: #66635d; background: #eae7e0; font-size: 12px; line-height: 18px; }
.user-edit__quick-tags text.active { color: #8c6c24; background: #f8f0dd; }
.user-edit__submit { display: flex; width: 100%; height: 52px; margin: 26px 0 0; padding: 0; align-items: center; justify-content: center; border: 0; border-radius: 999px; color: #fff; background: #bd9226; box-shadow: 0 12px 24px rgba(37,99,235,.3); font-size: 15px; font-weight: 600; }
.user-edit__submit:active { transform: scale(.98); background: #49443f; }
.user-edit__submit:disabled { opacity: .7; }
.user-edit { border: 1px solid #eae7e0; border-radius: 22px; background: #fff; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.user-edit__avatar { border-color: #fff; color: #242321; background: #eeeae2; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.user-edit__avatar--fallback { color: #242321; }
.user-edit__avatar-section button { border-color: #e6d7b5; color: #8c6c24; background: rgba(250,246,235,.88); }
.user-edit__field { border-color: #eae7e0; }
.user-edit__field > text { color: #77736c; }
.user-edit__field input, .user-edit__field textarea { border-color: #eae7e0; color: #242321; background: #faf9f6; }
.user-edit__gender button { border-color: #eae7e0; color: #77736c; background: #faf9f6; }
.user-edit__gender button.active { border-color: #242321; color: #fff; background: #242321; }
.user-edit__quick-tags text { color: #242321; background: #f7f5f0; }
.user-edit__quick-tags text.active { color: #8c6c24; background: #f8f0dd; }
.user-edit__submit { color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.user-edit__submit:active { background: #49443f; }
</style>

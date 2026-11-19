<template>
  <view class="team-form">
    <view class="team-form__row">
      <text class="team-form__label">队伍名</text>
      <input v-model="form.name" class="team-form__input" placeholder="请输入队伍名" />
    </view>
    <view class="team-form__row team-form__row--top">
      <text class="team-form__label">队伍描述</text>
      <textarea v-model="form.description" class="team-form__textarea" placeholder="请输入队伍描述" />
    </view>
    <view v-if="showMaxNum" class="team-form__row">
      <text class="team-form__label">最大人数</text>
      <view class="team-form__stepper">
        <button @click="changeMaxNum(-1)">－</button>
        <text>{{ form.maxNum }}</text>
        <button @click="changeMaxNum(1)">＋</button>
      </view>
    </view>
    <view class="team-form__row">
      <text class="team-form__label">过期日期</text>
      <picker class="team-form__picker" mode="date" :start="today" :value="form.expireDate" @change="form.expireDate = $event.detail.value">
        <view>{{ form.expireDate || '长期有效（点击选择）' }}</view>
      </picker>
      <button v-if="form.expireDate" class="team-form__clear" @click="form.expireDate = ''">清除</button>
    </view>
    <view v-if="form.expireDate" class="team-form__row">
      <text class="team-form__label">过期时间</text>
      <picker class="team-form__picker" mode="time" :value="form.expireTime" @change="form.expireTime = $event.detail.value">
        <view>{{ form.expireTime }}</view>
      </picker>
    </view>
    <view class="team-form__row">
      <text class="team-form__label">队伍状态</text>
      <view class="team-form__status">
        <button
          v-for="option in statusOptions"
          :key="option.value"
          :class="{ 'team-form__status--active': form.status === option.value }"
          @click="form.status = option.value"
        >{{ option.label }}</button>
      </view>
    </view>
    <view v-if="form.status === 2" class="team-form__row">
      <text class="team-form__label">队伍密码</text>
      <input v-model="form.password" class="team-form__input" type="password" placeholder="请输入队伍密码" />
    </view>

    <button class="team-form__submit" :loading="submitting" :disabled="submitting" @click="submit">
      {{ submitting ? '正在提交…' : submitText }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { TeamFormParams, TeamType } from '@/types/team'

const props = withDefaults(defineProps<{
  initial?: TeamType | null
  showMaxNum?: boolean
  submitText?: string
  submitting?: boolean
}>(), {
  initial: null,
  showMaxNum: true,
  submitText: '提交',
  submitting: false,
})

const emit = defineEmits<{ (event: 'submit', data: TeamFormParams): void }>()
const statusOptions = [
  { label: '公开', value: 0 },
  { label: '私有', value: 1 },
  { label: '加密', value: 2 },
]

const now = new Date()
const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
const form = reactive({
  id: 0,
  name: '',
  description: '',
  expireDate: '',
  expireTime: '23:59',
  maxNum: 3,
  password: '',
  status: 0,
})

watch(() => props.initial, (team) => {
  if (!team) return
  form.id = team.id
  form.name = team.name || ''
  form.description = team.description || ''
  form.maxNum = team.maxNum || 3
  form.password = team.password || ''
  form.status = Number(team.status ?? 0)
  if (team.expireTime) {
    const text = String(team.expireTime)
    form.expireDate = text.slice(0, 10)
    form.expireTime = text.slice(11, 16) || '23:59'
  }
}, { immediate: true })

function changeMaxNum(step: number) {
  form.maxNum = Math.min(10, Math.max(3, form.maxNum + step))
}

function submit() {
  if (!form.name.trim()) {
    uni.showToast({ title: '请输入队伍名', icon: 'none' })
    return
  }
  if (form.status === 2 && !form.password) {
    uni.showToast({ title: '请输入队伍密码', icon: 'none' })
    return
  }

  let expireTime: string | null = null
  if (form.expireDate) {
    const date = new Date(`${form.expireDate}T${form.expireTime || '23:59'}:00`)
    expireTime = Number.isNaN(date.getTime()) ? null : date.toISOString()
  }

  emit('submit', {
    ...(form.id ? { id: form.id } : {}),
    name: form.name.trim(),
    description: form.description.trim(),
    expireTime,
    ...(props.showMaxNum ? { maxNum: form.maxNum } : {}),
    password: form.password,
    status: Number(form.status),
  })
}
</script>

<style scoped>
.team-form {
  padding: 28rpx;
}

.team-form__row {
  display: flex;
  min-height: 104rpx;
  align-items: center;
  border-bottom: 1rpx solid #334155;
}

.team-form__row--top {
  align-items: flex-start;
}

.team-form__label {
  width: 160rpx;
  flex: 0 0 auto;
  color: #cbd5e1;
  font-size: 26rpx;
}

.team-form__input,
.team-form__textarea,
.team-form__picker {
  min-width: 0;
  flex: 1;
  color: #f8fafc;
  font-size: 27rpx;
}

.team-form__textarea {
  height: 160rpx;
  padding: 24rpx 0;
}

.team-form__stepper {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.team-form__stepper button,
.team-form__clear {
  margin: 0;
  padding: 0 24rpx;
  color: #f8fafc;
  background: #334155;
  font-size: 26rpx;
}

.team-form__status {
  display: flex;
  flex: 1;
  gap: 12rpx;
}

.team-form__status button {
  margin: 0;
  padding: 0 24rpx;
  color: #94a3b8;
  background: #1e293b;
  font-size: 24rpx;
}

.team-form__status .team-form__status--active {
  color: #0f172a;
  background: #f8fafc;
}

.team-form__submit {
  margin-top: 48rpx;
  color: #0f172a;
  background: #f8fafc;
  font-size: 28rpx;
}
</style>

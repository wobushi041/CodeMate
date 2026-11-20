<template>
  <view class="team-form-page">
    <view class="team-form-content">
      <view class="form-card">
        <view class="form-row">
          <text class="form-label">队伍名</text>
          <input v-model="form.name" class="form-control" placeholder="请输入队伍名" />
        </view>
        <view class="form-row form-row--textarea">
          <text class="form-label">队伍描述</text>
          <textarea v-model="form.description" class="form-control form-control--textarea" placeholder="请输入队伍描述" />
        </view>
        <view class="form-row">
          <text class="form-label">过期时间</text>
          <picker class="date-picker" mode="date" :start="today" :value="form.expireDate" @change="form.expireDate = $event.detail.value">
            <view class="form-control date-control">
              <view class="date-control__text" :class="{ 'date-control__text--placeholder': !form.expireDate }">{{ form.expireDate ? `${form.expireDate} ${form.expireTime}` : '点击选择过期时间' }}</view>
              <wd-icon class="date-control__icon" name="calendar" size="20px" />
            </view>
          </picker>
        </view>
        <view v-if="form.expireDate" class="form-row">
          <text class="form-label">具体时间</text>
          <picker class="date-picker" mode="time" :value="form.expireTime" @change="form.expireTime = $event.detail.value">
            <view class="form-control date-control">
              <view class="date-control__text">{{ form.expireTime }}</view>
              <wd-icon class="date-control__icon" name="time" size="20px" />
            </view>
          </picker>
          <button class="date-clear" @click="form.expireDate = ''">清除</button>
        </view>
        <view v-if="showMaxNum" class="form-row">
          <text class="form-label">最大人数</text>
          <view class="member-stepper">
            <button @click="changeMaxNum(-1)">－</button>
            <text>{{ form.maxNum }}</text>
            <button @click="changeMaxNum(1)">＋</button>
          </view>
        </view>
        <view class="form-row">
          <text class="form-label">队伍状态</text>
          <view class="status-options">
            <button
              v-for="option in statusOptions"
              :key="option.value"
              class="radio-option"
              @click="form.status = option.value"
            >
              <view class="radio-control" :class="{ 'radio-control--checked': form.status === option.value }">
                <view v-if="form.status === option.value" class="radio-control__dot" />
              </view>
              <text>{{ option.label }}</text>
            </button>
          </view>
        </view>
        <view v-if="form.status === 2" class="form-row">
          <text class="form-label">密码</text>
          <input v-model="form.password" class="form-control" type="password" placeholder="请输入队伍密码" />
        </view>
      </view>
    </view>

    <view class="team-form-dock">
      <button class="mall-action-btn" :loading="submitting" :disabled="submitting" @click="submit">
        {{ submitting ? '正在提交...' : submitText }}
      </button>
    </view>
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
.team-form-page, .team-form-content, .form-card, .form-row, .form-control, .date-picker, .member-stepper, .status-options, .radio-option, .team-form-dock, .mall-action-btn { box-sizing: border-box; }
.team-form-page { display: flex; width: 100%; min-width: 0; min-height: calc(100vh - 54px - var(--status-bar-height)); flex-direction: column; color: #242321; background: #faf9f6; }
.team-form-content { width: 100%; min-width: 0; padding: 12px 16px 24px; flex: 1; }
.form-card { display: flex; width: 100%; min-width: 0; padding: 24px; flex-direction: column; gap: 24px; border: 1px solid rgba(100,116,139,.5); border-radius: 16px; background: #ffffff; box-shadow: 0 18px 35px rgba(2,6,23,.22); }
.form-row { display: flex; width: 100%; min-width: 0; align-items: center; gap: 16px; }
.form-row--textarea { align-items: flex-start; }
.form-row--textarea .form-label { padding-top: 12px; }
.form-label { width: 80px; flex: 0 0 80px; color: #66635d; font-size: 14px; font-weight: 500; line-height: 20px; }
.form-control { width: 0; min-width: 0; height: 44px; padding: 0 16px; flex: 1 1 0; border: 1px solid #eae7e0; border-radius: 12px; color: #242321; background: #faf9f6; font-size: 14px; line-height: 42px; }
.form-control--textarea { height: 112px; min-height: 112px; padding: 14px 16px; line-height: 1.6; }
.date-picker { display: block; width: 0; min-width: 0; flex: 1 1 0; }
.date-picker .form-control { width: 100%; flex: none; }
.date-control { display: flex; align-items: center; justify-content: space-between; }
.date-control__text { min-width: 0; flex: 1; overflow: hidden; line-height: 42px; text-overflow: ellipsis; white-space: nowrap; }
.date-control__text--placeholder { color: #85817a; }
.date-control__icon { flex: 0 0 auto; color: #85817a; }
.date-clear { flex: 0 0 auto; margin: 0; padding: 0 4px; border: 0; color: #85817a; background: transparent; font-size: 12px; }
.date-clear::after, .member-stepper button::after, .radio-option::after, .mall-action-btn::after { border: 0; }
.member-stepper { display: flex; width: 0; min-width: 0; height: 44px; flex: 1 1 0; overflow: hidden; align-items: center; border: 1px solid #eae7e0; border-radius: 12px; background: #faf9f6; }
.member-stepper button { display: flex; width: 48px; height: 42px; margin: 0; padding: 0; flex: 0 0 48px; align-items: center; justify-content: center; border: 0; border-radius: 0; color: #85817a; background: transparent; font-size: 20px; }
.member-stepper button:first-child { border-right: 1px solid #eae7e0; }
.member-stepper button:last-child { border-left: 1px solid #eae7e0; }
.member-stepper text { display: flex; min-width: 0; flex: 1; align-items: center; justify-content: center; color: #242321; font-size: 14px; }
.status-options { display: flex; width: 0; min-width: 0; flex: 1 1 0; align-items: center; gap: 22px; }
.radio-option { display: flex; min-width: 0; margin: 0; padding: 0; flex: 0 0 auto; align-items: center; gap: 10px; border: 0; border-radius: 0; color: #242321; background: transparent; font-size: 14px; line-height: 20px; white-space: nowrap; }
.radio-control { display: flex; width: 20px; height: 20px; flex: 0 0 20px; align-items: center; justify-content: center; border: 2px solid #85817a; border-radius: 50%; background: transparent; }
.radio-control--checked { border-color: #fff; }
.radio-control__dot { width: 10px; height: 10px; border-radius: 50%; background: #fff; }
.team-form-dock { width: 100%; padding: 12px 16px calc(14px + env(safe-area-inset-bottom)); flex: 0 0 auto; background: linear-gradient(180deg,rgba(15,23,42,0),#faf9f6 24%); }
.mall-action-btn { display: flex; width: 100%; height: 52px; margin: 0; padding: 0; align-items: center; justify-content: center; gap: 10px; border: 0; border-radius: 999px; color: #fff; background: #bd9226; box-shadow: 0 12px 24px rgba(37,99,235,.3); font-size: 15px; font-weight: 600; }
.mall-action-btn:active { transform: scale(.98); background: #49443f; }
.mall-action-btn:disabled { opacity: .7; }
@media (max-width:360px) { .form-card { padding: 20px 16px; } .form-row { gap: 10px; } .form-label { width: 68px; flex-basis: 68px; } .status-options { gap: 12px; } .radio-option { gap: 6px; } }
.form-card { border: 1px solid #eae7e0; border-radius: 22px; background: #fff; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.form-label { color: #242321; }
.form-control, .member-stepper { border-color: #eae7e0; color: #242321; background: #faf9f6; box-shadow: none; }
.date-control__icon, .member-stepper button { color: #77736c; }
.member-stepper button:first-child, .member-stepper button:last-child { border-color: #eae7e0; }
.member-stepper text, .radio-option { color: #242321; }
.radio-control { border-color: #a9a49b; }
.radio-control--checked { border-color: #bd9226; }
.radio-control__dot { background: #bd9226; }
.team-form-dock { border-top: 1px solid #eae7e0; background: rgba(250,249,246,.92); }
.mall-action-btn { color: #fff; background: #242321; box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.mall-action-btn:active { background: #49443f; }
</style>

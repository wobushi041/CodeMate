<template>
  <view v-if="loading" class="partner-card partner-card--loading">
    <view class="loading-avatar" />
    <view class="loading-lines"><view /><view /><view /></view>
  </view>

  <view v-else-if="user" class="partner-card">
    <view class="partner-avatar-wrap">
      <image class="partner-avatar" :src="avatarUrl" mode="aspectFill" @error="avatarError = true" />
    </view>
    <view class="partner-info">
      <view>
        <view class="partner-name">{{ displayName }}</view>
        <view class="partner-subtitle">寻找项目队友</view>
        <view class="partner-tags">
          <text v-for="tag in tags" :key="tag">{{ tag }}</text>
          <text v-if="!tags.length">暂未设置标签</text>
        </view>
      </view>
      <view class="contact-row">
        <button
          :class="{ 'contact-button--contacted': contacted }"
          :disabled="contacting"
          @click.stop="emit('contact', user)"
        >
          <wd-icon v-if="contacting" name="refresh" class="is-spinning" size="16px" />
          <wd-icon v-else-if="!contacted" name="jump" size="16px" />
          <wd-icon v-else name="check" size="16px" />
          <text>{{ contacting ? '发起中...' : contacted ? '已联系' : '联系我' }}</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { UserType } from '@/types/api'
import { normalizeTags } from '@/utils/format'

const props = withDefaults(defineProps<{
  user?: UserType
  contacted?: boolean
  contacting?: boolean
  loading?: boolean
}>(), {
  contacted: false,
  contacting: false,
  loading: false,
})

const emit = defineEmits<{ (event: 'contact', user: UserType): void }>()
const fallbackAvatar = 'https://api.dicebear.com/8.x/initials/svg?seed=Code&backgroundColor=334155&fontFamily=Arial'
const avatarError = ref(false)
watch(() => props.user?.id, () => { avatarError.value = false })
const avatarUrl = computed(() => avatarError.value ? fallbackAvatar : props.user?.avatarUrl || fallbackAvatar)
const tags = computed(() => normalizeTags(props.user?.tags))
const displayName = computed(() => {
  if (!props.user) return ''
  const name = props.user.username || props.user.userAccount || '编程伙伴'
  return props.user.planetCode ? `${name}(${props.user.planetCode})` : name
})
</script>

<style scoped>
.partner-card { display: flex; box-sizing: border-box; width: 100%; max-width: 100%; min-width: 0; min-height: 112px; gap: 16px; padding: 16px; overflow: hidden; border: 1px solid transparent; border-radius: 16px; background: #ffffff; transition: transform .15s ease, box-shadow .15s ease; }
.partner-avatar-wrap { position: relative; width: 80px; height: 80px; flex: 0 0 auto; }
.partner-avatar { display: block; width: 80px; height: 80px; border-radius: 12px; background: #eae7e0; }
.partner-info { display: flex; width: 0; min-width: 0; flex: 1 1 0; flex-direction: column; justify-content: space-between; }
.partner-info > view { min-width: 0; }
.partner-name { width: 100%; max-width: 100%; overflow: hidden; color: #242321; font-size: 16px; font-weight: 600; line-height: 20px; text-overflow: ellipsis; white-space: nowrap; }
.partner-subtitle { margin-top: 4px; color: #66635d; font-size: 12px; line-height: 16px; }
.partner-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.partner-tags text { padding: 4px 12px; border-radius: 999px; color: #66635d; background: #eae7e0; font-size: 12px; line-height: 16px; }
.contact-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.contact-row button { display: inline-flex; max-width: 100%; min-height: 36px; margin: 0; padding: 8px 20px; align-items: center; gap: 8px; border: 0; border-radius: 999px; color: #242321; background: #fff; font-size: 14px; font-weight: 600; line-height: 20px; box-shadow: 0 4px 8px rgba(2,6,23,.18); }
.contact-row button::after { border: 0; }
.contact-row button:active { transform: scale(.95); background: #f4f4f5; }
.contact-row button.contact-button--contacted { color: #447153; background: rgba(34,197,94,.14); }
.contact-row button:disabled { opacity: .65; }
.partner-card--loading { min-height: 144px; }
.loading-avatar { width: 80px; height: 80px; flex: 0 0 80px; border-radius: 12px; background: #eae7e0; animation: loading-pulse 1.4s ease-in-out infinite; }
.loading-lines { display: grid; flex: 1; align-content: start; gap: 10px; padding-top: 4px; }
.loading-lines view { display: block; height: 10px; border-radius: 999px; background: #eae7e0; animation: loading-pulse 1.4s ease-in-out infinite; }
.loading-lines view:nth-child(2) { width: 72%; }
.loading-lines view:nth-child(3) { width: 48%; }
.is-spinning { animation: spin .8s linear infinite; }
@keyframes loading-pulse { 0%,100% { opacity: .5; } 50% { opacity: 1; } }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.partner-card { border: 1px solid #eae7e0; border-radius: 20px; background: rgba(255,255,255,.92); box-shadow: 0 12px 34px rgba(39,34,27,.07); }
.partner-avatar { border-radius: 14px; background: #ebe8e1; }
.partner-name { color: #242321; font-size: 17px; }
.partner-subtitle { color: #77736c; }
.partner-tags text { border: 1px solid #eee8db; color: #6f654c; background: #f8f0dd; }
.contact-row button { color: #fff; background: #242321; box-shadow: 0 6px 14px rgba(39,34,27,.12); }
.contact-row button:active { background: #49443f; }
.contact-row button.contact-button--contacted { border: 1px solid #b9d7c0; color: #447153; background: #edf6ef; }
.loading-avatar, .loading-lines view { background: #eeeae2; }
</style>

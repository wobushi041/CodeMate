<template>
  <view class="partner-card">
    <view class="partner-card__main">
      <image v-if="user.avatarUrl" class="partner-card__avatar" :src="user.avatarUrl" mode="aspectFill" />
      <view v-else class="partner-card__avatar partner-card__avatar--fallback">{{ displayName.slice(0, 1) }}</view>
      <view class="partner-card__content">
        <view class="partner-card__heading">
          <text class="partner-card__name">{{ displayName }}</text>
          <text v-if="user.planetCode" class="partner-card__code">#{{ user.planetCode }}</text>
        </view>
        <text class="partner-card__account">{{ user.userAccount || '编程伙伴' }}</text>
        <view class="partner-card__tags">
          <text v-for="tag in tags" :key="tag" class="partner-card__tag">{{ tag }}</text>
          <text v-if="!tags.length" class="partner-card__tag">暂未设置标签</text>
        </view>
      </view>
    </view>
    <button
      class="partner-card__contact"
      :class="{ 'partner-card__contact--done': contacted }"
      :disabled="contacting"
      @click="emit('contact', user)"
    >
      {{ contacting ? '正在打开…' : contacted ? '继续沟通' : '联系 TA' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserType } from '@/types/api'
import { normalizeTags } from '@/utils/format'

const props = withDefaults(defineProps<{
  user: UserType
  contacted?: boolean
  contacting?: boolean
}>(), {
  contacted: false,
  contacting: false,
})

const emit = defineEmits<{
  (event: 'contact', user: UserType): void
}>()

const tags = computed(() => normalizeTags(props.user.tags))
const displayName = computed(() => props.user.username || props.user.userAccount || '编程伙伴')
</script>

<style scoped>
.partner-card {
  margin-bottom: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #334155;
  border-radius: 28rpx;
  background: #1e293b;
}

.partner-card__main {
  display: flex;
  align-items: flex-start;
}

.partner-card__avatar {
  display: flex;
  width: 112rpx;
  height: 112rpx;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  background: #334155;
}

.partner-card__avatar--fallback {
  color: #f8fafc;
  font-size: 42rpx;
  font-weight: 700;
}

.partner-card__content {
  min-width: 0;
  margin-left: 24rpx;
  flex: 1;
}

.partner-card__heading {
  display: flex;
  align-items: center;
}

.partner-card__name {
  overflow: hidden;
  max-width: 320rpx;
  font-size: 32rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.partner-card__code {
  margin-left: 12rpx;
  color: #64748b;
  font-size: 22rpx;
}

.partner-card__account {
  display: block;
  margin-top: 8rpx;
  color: #94a3b8;
  font-size: 24rpx;
}

.partner-card__tags {
  display: flex;
  margin-top: 16rpx;
  flex-wrap: wrap;
  gap: 10rpx;
}

.partner-card__tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  color: #cbd5e1;
  background: #334155;
  font-size: 21rpx;
}

.partner-card__contact {
  width: 100%;
  height: 72rpx;
  margin-top: 24rpx;
  border-radius: 18rpx;
  color: #0f172a;
  background: #f8fafc;
  font-size: 26rpx;
  line-height: 72rpx;
}

.partner-card__contact--done {
  color: #f8fafc;
  background: #334155;
}
</style>

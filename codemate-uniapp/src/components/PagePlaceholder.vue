<template>
  <view class="placeholder">
    <view class="placeholder__mark">{{ mark }}</view>
    <text class="placeholder__title">{{ title }}</text>
    <text class="placeholder__description">{{ description }}</text>
    <slot />
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description: string
  mark?: string
}>(), {
  mark: 'CM',
})
</script>

<style scoped>
.placeholder {
  display: flex;
  min-height: 70vh;
  padding: 96rpx 48rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #242321;
}

.placeholder__mark {
  display: flex;
  width: 112rpx;
  height: 112rpx;
  margin-bottom: 32rpx;
  align-items: center;
  justify-content: center;
  border: 1rpx solid #85817a;
  border-radius: 32rpx;
  color: #242321;
  background: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
}

.placeholder__title {
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.4;
}

.placeholder__description {
  max-width: 560rpx;
  margin-top: 20rpx;
  color: #85817a;
  font-size: 28rpx;
  line-height: 1.7;
  text-align: center;
}
</style>

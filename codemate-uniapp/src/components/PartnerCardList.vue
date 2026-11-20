<template>
  <view class="partner-list"><slot /></view>
</template>

<style scoped>
.partner-list {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}
</style>

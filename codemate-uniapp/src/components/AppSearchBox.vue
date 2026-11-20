<template>
  <button v-if="trigger" class="partner-search partner-search--trigger" @click="emit('activate')">
    <wd-icon name="search" size="20px" />
    <text>{{ placeholder }}</text>
  </button>
  <view v-else class="partner-search">
    <wd-icon name="search" size="20px" />
    <input
      :value="modelValue"
      type="text"
      :placeholder="placeholder"
      confirm-type="search"
      @input="onInput"
      @confirm="onSearch"
    />
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  trigger?: boolean
}>(), {
  modelValue: '',
  placeholder: '请输入搜索内容',
  trigger: false,
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'search', value: string): void
  (event: 'activate'): void
}>()

function onInput(event: any) {
  emit('update:modelValue', event.detail.value)
}

function onSearch(event: any) {
  emit('search', String(event.detail.value || '').trim())
}
</script>

<style scoped>
.partner-search {
  display: flex;
  width: 100%;
  height: 48px;
  margin-bottom: 24px;
  padding: 0 16px;
  align-items: center;
  gap: 12px;
  border: 1px solid #eae7e0;
  border-radius: 16px;
  color: #85817a;
  background: #ffffff;
  font-size: 14px;
  line-height: 20px;
  text-align: left;
}
.partner-search--trigger:active { transform: scale(.99); }
.partner-search--trigger::after { border: 0; }
.partner-search text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.partner-search input { min-width: 0; height: 46px; flex: 1; color: #242321; background: transparent; font-size: 14px; }
.partner-search { border: 1px solid #eae7e0; border-radius: 16px; color: #77736c; background: rgba(255,255,255,.9); box-shadow: 0 7px 22px rgba(39,34,27,.04); }
.partner-search input { color: #242321; }
</style>
